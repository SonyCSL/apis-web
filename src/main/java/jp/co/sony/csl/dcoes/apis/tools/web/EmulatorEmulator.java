package jp.co.sony.csl.dcoes.apis.tools.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.JsonObjectUtil;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.JsonObjectWrapper;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.VertxConfig;

/**
 * 外部に対しユニットデータを提供する Verticle.
 * {@link jp.co.sony.csl.dcoes.apis.tools.web.util.Starter} Verticle から起動される.
 * DCDC emulator 環境での main_controller へのユニットデータ提供を模倣.
 * 以下の API を提供する.
 * - /get/log : 全ユニットのユニットデータを取得する
 * 環境変数に {@code removeDeprecatedData=true} が指定されていたら以下の属性を削除して返す.
 * - emu 以下
 * - dcdc.powermeter 以下
 * @author OES Project
 */
public class EmulatorEmulator extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(EmulatorEmulator.class);

	/**
	 * サービスを開くポートのデフォルト値.
	 * 値は {@value}
	 */
	private static final int DEFAULT_PORT = 43900;

	/**
	 * ユニットデータのキャッシュ.
	 * 定期的に GridMaster に問合せキャッシュしておく.
	 */
	public static final JsonObjectWrapper cache = new JsonObjectWrapper();
	private static final JsonObject empty_ = new JsonObject();

	/**
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
	 * 停止時に呼び出される.
	 * @throws Exception {@inheritDoc}
	 */
	@Override public void stop() throws Exception {
		if (log.isTraceEnabled()) log.trace("stopped : " + deploymentID());
	}

	////

	/**
	 * HTTP サービスを起動する.
	 * CONFIG から設定を取得し初期化する.
	 * - CONFIG.emulatorEmulator.port : ポート [{@link Integer}]
	 * @param completionHandler the completion handler
	 */
	private void startHttpService_(Handler<AsyncResult<Void>> completionHandler) {
		Integer port = VertxConfig.config.getInteger(DEFAULT_PORT, "emulatorEmulator", "port");
		vertx.createHttpServer().requestHandler(req -> {
			req.exceptionHandler(t -> {
				log.error("exceptionHandler : " + t);
				JsonObject result = new JsonObject().put("error", "exceptionHandler : " + t);
				req.response().setChunked(true).putHeader("content-type", "application/json").setStatusCode(400).end(result.encode() + '\n');
			});
			try {
				if (req.path().equals("/get/log")) {
					vertx.eventBus().<JsonObject>send(ServiceAddress.GridMaster.unitDatas(), null, rep -> {
						JsonObject result = null;
						if (rep.succeeded()) {
							result = rep.result().body();
							if (result != null) {
								if (log.isDebugEnabled()) log.debug("size of result : " + result.size());
								if (Boolean.valueOf(System.getenv("removeDeprecatedData"))) removeDeprecatedData_(result);
								cache.setJsonObject(result);
							} else {
								if (log.isWarnEnabled()) log.warn("result is null");
							}
						} else {
							log.error("Communication failed on EventBus ; " + rep.cause());
						}
						if (result == null) {
							if (!cache.isNull()) {
								if (log.isWarnEnabled()) log.warn("size of cache : " + cache.jsonObject().size());
							} else {
								if (log.isWarnEnabled()) log.warn("cache is null");
							}
							result = (!cache.isNull()) ? cache.jsonObject() : empty_;
						}
						req.response().setChunked(true).putHeader("content-type", "application/json").end(result.encode() + '\n');
					});
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
				if (log.isInfoEnabled()) log.info("emulator emulation http service started on port : " + port);
				completionHandler.handle(Future.succeededFuture());
			} else {
				log.error(res.cause());
				completionHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}

	/**
	 * 環境変数に {@code removeDeprecatedData=true} が指定されていた場合に呼ばれユニットデータをコンバートする.
	 * 以下の属性を削除して返す.
	 * - emu 以下
	 * - dcdc.powermeter 以下
	 * @param data コンバート対象のユニットデータ
	 */
	private void removeDeprecatedData_(JsonObject data) {
		for (String aUnitId : data.fieldNames()) {
			JsonObject aUnitData = data.getJsonObject(aUnitId);
			aUnitData.remove("emu");
			JsonObjectUtil.remove(aUnitData, "dcdc", "powermeter");
		}
	}

}
