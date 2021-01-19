package jp.co.sony.csl.dcoes.apis.tools.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.co.sony.csl.dcoes.apis.common.Deal;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.common.util.StringUtil;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.VertxConfig;

/**
 * This Verticle provides Power Sharing conditions, etc., to the outside.
 * Started from {@link jp.co.sony.csl.dcoes.apis.tools.web.util.Starter} Verticle.
 * Emulates providing {@code BUDO} information to main_controller in the DCDC emulator environment.
 * Provides the following API.
 * - /shutdown         : Shuts down unit specified by {@code unitId} or all units (including apis-tools participating in clusters).
 * - /setOperationMode : Changes the local Power Sharing mode or global Power Sharing mode of the unit specified by {@code unitId} to {@code value}
 * - /deals            : Gets Power Sharing information
 * - /unitIds          : Gets list of IDs of units set by POLICY 
 * - /getStatus        : Gets whether of not the global Power Sharing mode is a mode that allows Power Sharing
 * - /active           : Sets global Power Sharing mode to {@code autonomous}
 * - /quiet            : Sets global Power Sharing mode to {@code heteronomous}
 * - /stop             : Sets global Power Sharing mode to {@code stop}
 * - /manual           : Sets global Power Sharing mode to {@code manual}
 * @author OES Project
 * 外部に対し融通状況などを提供する Verticle.
 * {@link jp.co.sony.csl.dcoes.apis.tools.web.util.Starter} Verticle から起動される.
 * DCDC emulator 環境での main_controller への {@code BUDO} 情報提供を模倣.
 * 以下の API を提供する.
 * - /shutdown         : {@code unitId} で指定したユニットまたは全ユニット ( クラスタに参加する apis-tools 含む ) をシャットダウンする
 * - /setOperationMode : {@code unitId} で指定したユニットのローカル融通モードまたはグローバル融通モードを {@code value} に変更する
 * - /deals            : 融通情報を取得する
 * - /unitIds          : POLICY で設定されたユニットの ID のリストを取得する
 * - /getStatus        : グローバル融通モードが融通可能な状態か否かを取得する
 * - /active           : グローバル融通モードを {@code autonomous} に設定する
 * - /quiet            : グローバル融通モードを {@code heteronomous} に設定する
 * - /stop             : グローバル融通モードを {@code stop} に設定する
 * - /manual           : グローバル融通モードを {@code manual} に設定する
 * @author OES Project
 */
public class BudoEmulator extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(BudoEmulator.class);

	/**
	 * This is the default value of the port that opens the service.
	 * The value is {@value}
	 * サービスを開くポートのデフォルト値.
	 * 値は {@value}
	 */
	private static final int DEFAULT_PORT = 43830;

	/**
	 * Called during startup.
	 * Opens HTTP service.
	 * 起動時に呼び出される.
	 * HTTP サービスを開く.
	 * @param startFuture {@inheritDoc}
	 * @throws Exception {@inheritDoc}
	 */
	@Override public void start(Future<Void> startFuture) throws Exception {
		startHttpService_(resHttp -> {
			if (resHttp.succeeded()) {
				if (log.isTraceEnabled()) log.trace("started : " + deploymentID());
				startFuture.complete();
			} else {
				startFuture.fail(resHttp.cause());
			}
		});
	}

	/**
	 * Called when stopped.
	 * @throws Exception {@inheritDoc}
	 * 停止時に呼び出される.
	 * @throws Exception {@inheritDoc}
	 */
	@Override public void stop() throws Exception {
		if (log.isTraceEnabled()) log.trace("stopped : " + deploymentID());
	}

	////

	/**
	 * Starts HTTP service.
	 * Gets settings from CONFIG and initializes.
	 * - CONFIG.budoEmulator.port : ポート [{@link Integer}]
	 * @param completionHandler The completion handler
	 * HTTP サービスを起動する.
	 * CONFIG から設定を取得し初期化する.
	 * - CONFIG.budoEmulator.port : ポート [{@link Integer}]
	 * @param completionHandler the completion handler
	 */
	private void startHttpService_(Handler<AsyncResult<Void>> completionHandler) {
		Integer port = VertxConfig.config.getInteger(DEFAULT_PORT, "budoEmulator", "port");
		vertx.createHttpServer().requestHandler(req -> {
			req.exceptionHandler(t -> {
				log.error("exceptionHandler : " + t);
				JsonObject result = new JsonObject().put("error", "exceptionHandler : " + t);
				req.response().setChunked(true).putHeader("content-type", "application/json").setStatusCode(400).end(result.encode() + '\n');
			});
			try {
				if (req.path().equals("/shutdown")) {
					String unitId = StringUtil.nullIfEmpty(req.getParam("unitId"));
					if (unitId != null) {
						vertx.eventBus().send(ServiceAddress.shutdown(unitId), null, rep -> {
							if (rep.succeeded()) {
								JsonObject result = new JsonObject().put("succeeded", Boolean.TRUE);
								req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
							} else {
								log.error("Communication failed on EventBus ; " + rep.cause());
								JsonObject result = new JsonObject().put("succeeded", Boolean.FALSE).put("error", String.valueOf(rep.cause()));
								req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
							}
						});
					} else {
						vertx.eventBus().publish(ServiceAddress.shutdownAll(), null);
						JsonObject result = new JsonObject().put("succeeded", Boolean.TRUE);
						req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
					}
				} else if (req.path().equals("/setOperationMode")) {
					String unitId = StringUtil.nullIfEmpty(req.getParam("unitId"));
					String value = StringUtil.nullIfEmpty(req.getParam("value"));
					String address = (unitId != null) ? ServiceAddress.User.operationMode(unitId) : ServiceAddress.operationMode();
					if (log.isInfoEnabled()) log.info("setting " + ((unitId != null) ? "unit " + unitId + "'s" : "global" ) + " operationMode : " + value + " ...");
					DeliveryOptions options = new DeliveryOptions().addHeader("command", "set");
					vertx.eventBus().send(address, value, options, rep -> {
						if (rep.succeeded()) {
							JsonObject result = new JsonObject().put("succeeded", Boolean.TRUE);
							req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
						} else {
							log.error("Communication failed on EventBus ; " + rep.cause());
							JsonObject result = new JsonObject().put("succeeded", Boolean.FALSE).put("error", String.valueOf(rep.cause()));
							req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
						}
					});
				} else if (req.path().equals("/deals")) {
					vertx.eventBus().<JsonArray>send(ServiceAddress.Mediator.deals(), null, rep -> {
						if (rep.succeeded()) {
							JsonArray deals = rep.result().body();
							JsonArray result = new JsonArray();
							for (Object obj : deals) {
								if (obj instanceof JsonObject) {
									JsonObject aDealInfo = convertDealInfo_((JsonObject) obj);
									result.add(aDealInfo);
								}
							}
							req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
						} else {
							log.error("Communication failed on EventBus ; " + rep.cause());
							JsonArray result = new JsonArray();
							req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
						}
					});
				} else if (req.path().equals("/unitIds")) {
					vertx.eventBus().<JsonArray>send(ServiceAddress.GridMaster.unitIds(), null, rep -> {
						if (rep.succeeded()) {
							JsonArray result = rep.result().body();
							req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
						} else {
							log.error("Communication failed on EventBus ; " + rep.cause());
							JsonArray result = new JsonArray();
							req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
						}
					});
				} else if (req.path().equals("/getStatus")) {
					checkGlobalOperationModeAndReturn_(req);
				} else if (req.path().equals("/active")) {
					setGlobalOperationModeAndReturn_(req, "autonomous");
				} else if (req.path().equals("/quiet")) {
					setGlobalOperationModeAndReturn_(req, "heteronomous");
				} else if (req.path().equals("/stop")) {
					setGlobalOperationModeAndReturn_(req, "stop");
				} else if (req.path().equals("/manual")) {
					setGlobalOperationModeAndReturn_(req, "manual");
				} else {
					if (log.isWarnEnabled()) log.warn("not found : " + req.uri());
					JsonObject result = new JsonObject().put("error", "not found : " + req.uri());
					req.response().setChunked(true).putHeader("content-type", "application/json").setStatusCode(404).end(result.encode() + '\n');
				}
			} catch (Exception e) {
				log.error("exception : " + e);
				JsonObject result = new JsonObject().put("error", "exception : " + e);
				req.response().setChunked(true).putHeader("content-type", "application/json").setStatusCode(400).end(result.encode() + '\n');
			}
		}).listen(port, res -> {
			if (res.succeeded()) {
				if (log.isInfoEnabled()) log.info("budo emulation http service started on port : " + port);
				completionHandler.handle(Future.succeededFuture());
			} else {
				log.error(res.cause());
				completionHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}

	////

	/**
	 * Sets the global Power Sharing mode, gets the global Power Sharing mode again, and returns the response.
	 * @param req HTTP Request object
	 * @param globalOperationMode Global Power Sharing mode to be set
	 * グローバル融通モードをセットしあらためてグローバル融通モードを取得してレスポンスを返す.
	 * @param req HTTP リクエストオブジェクト
	 * @param globalOperationMode セットするグローバル融通モード
	 */
	private void setGlobalOperationModeAndReturn_(HttpServerRequest req, String globalOperationMode) {
		if (log.isInfoEnabled()) log.info("setting global operationMode : '" + globalOperationMode + "' ...");
		DeliveryOptions options = new DeliveryOptions().addHeader("command", "set");
		// Sets Global Power Sharing mode
		// グローバル融通モードをセットする
		vertx.eventBus().send(ServiceAddress.operationMode(), globalOperationMode, options, rep -> {
			if (rep.succeeded()) {
				// Gets global Power Sharing mode and returns the response
				// グローバル融通モードを取得してレスポンスを返す
				checkGlobalOperationModeAndReturn_(req);
			} else {
				log.error("Communication failed on EventBus ; " + rep.cause());
				JsonObject result = new JsonObject().put("active", Boolean.FALSE).put("error", String.valueOf(rep.cause()));
				req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
			}
		});
	}
	/**
	 * Gets global Power Sharing mode and returns the response.
	 * @param req HTTP Request object
	 * グローバル融通モードを取得してレスポンスを返す.
	 * @param req HTTP リクエストオブジェクト
	 */
	private void checkGlobalOperationModeAndReturn_(HttpServerRequest req) {
		checkGlobalOperationMode_(res -> {
			if (res.succeeded()) {
				// Returns true if autonomous or heteronomous, otherwise returns false
				// autonomous または heteronomous で true, それ以外は false を返すことになる
				JsonObject result = new JsonObject().put("active", res.result());
				req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
			} else {
				// Returns false if error occurs
				// エラーが起きたら false を返す
				JsonObject result = new JsonObject().put("active", Boolean.FALSE).put("error", String.valueOf(res.cause()));
				req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
			}
		});
	}
	/**
	 * Gets global Power Sharing mode.
	 * Returns {@link Boolean#TRUE} if {@code autonomous} or {@code heteronomous}, otherwise returns {@link Boolean#FALSE}.
	 * @param completionHandler The completion handler
	 * グローバル融通モードを取得する.
	 * {@code autonomous} または {@code heteronomous} なら {@link Boolean#TRUE}, それ以外なら {@link Boolean#FALSE} を返す.
	 * @param completionHandler the completion handler
	 */
	private void checkGlobalOperationMode_(Handler<AsyncResult<Boolean>> completionHandler) {
		DeliveryOptions options = new DeliveryOptions().addHeader("command", "get");
		vertx.eventBus().<String>send(ServiceAddress.operationMode(), null, options, repOperationMode -> {
			if (repOperationMode.succeeded()) {
				String operationMode = repOperationMode.result().body();
				if ("autonomous".equals(operationMode) || "heteronomous".equals(operationMode)) {
					vertx.eventBus().<Boolean>send(ServiceAddress.GridMaster.errorTesting(), null, repErrorTesting -> {
						if (repErrorTesting.succeeded()) {
							Boolean hasErrors = repErrorTesting.result().body();
							if (Boolean.FALSE.equals(hasErrors)) {
								completionHandler.handle(Future.succeededFuture(Boolean.TRUE));
							} else {
								completionHandler.handle(Future.succeededFuture(Boolean.FALSE));
							}
						} else {
							log.error("Communication failed on EventBus ; " + repErrorTesting.cause());
							completionHandler.handle(Future.failedFuture(repErrorTesting.cause()));
						}
					});
				} else {
					completionHandler.handle(Future.succeededFuture(Boolean.FALSE));
				}
			} else {
				log.error("Communication failed on EventBus ; " + repOperationMode.cause());
				completionHandler.handle(Future.failedFuture(repOperationMode.cause()));
			}
		});
	}

	/**
	 * Converts APIS DEAL object to ancient BUDO system's Power Sharing information.
	 * @param deal DEAL object
	 * @return Power Sharing information {@link JsonObject} belonging to ancient BUDO system era
	 * APIS の DEAL オブジェクトからいにしえの BUDO システム時代の融通情報に変換する.
	 * @param deal DEAL オブジェクト
	 * @return いにしえの BUDO システム時代の融通情報 {@link JsonObject}
	 */
	private JsonObject convertDealInfo_(JsonObject deal) {
		String chargingUnit = Deal.chargeUnitId(deal);
		String dischargingUnit = Deal.dischargeUnitId(deal);
		Boolean isMasterDeal = Deal.isMaster(deal);
		String request = Deal.type(deal);
		String requester = Deal.requestUnitId(deal);
		String responder = Deal.acceptUnitId(deal);
		String startTime = Deal.createDateTime(deal);
		JsonObject result = new JsonObject();
		if (chargingUnit != null) result.put("chargingUnit", chargingUnit);
		if (dischargingUnit != null) result.put("dischargingUnit", dischargingUnit);
		if (isMasterDeal != null) result.put("isMasterDeal", isMasterDeal);
		if (request != null) result.put("request", request.toUpperCase());
		if (requester != null) result.put("requester", requester);
		if (responder != null) result.put("responder", responder);
		if (startTime != null) result.put("startTime", startTime);
		return result;
	}

}
