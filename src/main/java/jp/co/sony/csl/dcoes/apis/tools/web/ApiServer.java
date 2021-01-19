package jp.co.sony.csl.dcoes.apis.tools.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.VertxConfig;
import jp.co.sony.csl.dcoes.apis.tools.web.api_handler.DealGeneration;
import jp.co.sony.csl.dcoes.apis.tools.web.api_handler.ErrorGeneration;
import jp.co.sony.csl.dcoes.apis.tools.web.api_handler.LogConfiguration;

/**
 * These Verticles provide various Web API for controlling APIS from the outside.
 * Started from {@link jp.co.sony.csl.dcoes.apis.tools.web.util.Starter} Verticle.
 * Provides the following services.
 * - {@link ErrorGeneration} : Provides Web API for delivering errors from the outside.
 * - {@link DealGeneration} : Provides Web API for delivering Power Sharing information from the outside.
 * - {@link LogConfiguration} : Provides Web API for changing the log level from the outside. 
 * @author OES Project
 * 外部から APIS を制御するための各種 Web API を提供する Verticle. 
 * {@link jp.co.sony.csl.dcoes.apis.tools.web.util.Starter} Verticle から起動される.
 * 以下のサービスを提供する.
 * - {@link ErrorGeneration} : 外部からエラーを投入するための Web API を提供する
 * - {@link DealGeneration} : 外部から融通情報を投入するための Web API を提供する
 * - {@link LogConfiguration} : 外部からログのレベルを変更するための Web API を提供する
 * @author OES Project
 */
public class ApiServer extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

	/**
	 * This is the default value of the port for opening the service.
	 * Value is {@value}
	 * サービスを開くポートのデフォルト値.
	 * 値は {@value}
	 */
	private static final int DEFAULT_PORT = 9999;

	/**
	 * Array of objects that process the API.
	 * API を処理するオブジェクトの配列.
	 */
	private static final ApiHandler[] apiHandlers_ = new ApiHandler[] {
		new ErrorGeneration(),
		new DealGeneration(),
		new LogConfiguration(),
	};

	/**
	 * Called during startup.
	 * Opens the HTTP service.
	 * @param startFuture {@inheritDoc}
	 * @throws Exception {@inheritDoc}
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
	 * Starts the HTTP service.
	 * Gets settings from CONFIG and initializes.
	 * - CONFIG.apiServer.port : Port [{@link Integer}]
	 * @param completionHandler The completion handler
	 * HTTP サービスを起動する.
	 * CONFIG から設定を取得し初期化する.
	 * - CONFIG.apiServer.port : ポート [{@link Integer}]
	 * @param completionHandler the completion handler
	 */
	private void startHttpService_(Handler<AsyncResult<Void>> completionHandler) {
		Integer port = VertxConfig.config.getInteger(DEFAULT_PORT, "apiServer", "port");
		vertx.createHttpServer().requestHandler(req -> {
			req.exceptionHandler(t -> {
				log.error("exceptionHandler : " + t);
				req.response().setChunked(true).putHeader("content-type", "text/plain").setStatusCode(500).end("exceptionHandler : " + t + '\n');
			});
			try {
				for (ApiHandler apiHandler : apiHandlers_) {
					if (apiHandler.canHandleRequest(req)) {
						apiHandler.handleRequest(vertx, req, log);
						return;
					}
				}
				if (log.isWarnEnabled()) log.warn("not found : " + req.uri());
				req.response().setStatusCode(404).end();
			} catch (Exception e) {
				log.error("exception : " + e);
				req.response().setChunked(true).putHeader("content-type", "text/plain").setStatusCode(500).end("exception : " + e + '\n');
			}
		}).listen(port, res -> {
			if (res.succeeded()) {
				if (log.isInfoEnabled()) log.info("api service started on port : " + port);
				completionHandler.handle(Future.succeededFuture());
			} else {
				log.error(res.cause());
				completionHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}

	/**
	 * This is the interface for cealling the object to be implemented for API.
	 * @author OES Project
	 * API の実装オブジェクトを呼び出すためのインタフェイス.
	 * @author OES Project
	 */
	public static interface ApiHandler {
		/**
		 * Confirms that it is possible to process the received request.
		 * @param req Request {@link HttpServerRequest}
		 * @return Yes/no flag
		 * 受信したリクエストを処理可能か確認する.
		 * @param req リクエスト {@link HttpServerRequest}
		 * @return 可否フラグ
		 */
		boolean canHandleRequest(HttpServerRequest req);
		/**
		 * Processes the received request.
		 * @param vertx vertx object
		 * @param req Request {@link HttpServerRequest}
		 * @param log Logger {@link Logger}
		 * 受信したリクエストに対する処理を実行する.
		 * @param vertx vertx オブジェクト
		 * @param req リクエスト {@link HttpServerRequest}
		 * @param log ロガー {@link Logger}
		 */
		void handleRequest(Vertx vertx, HttpServerRequest req, Logger log);
	}

}
