package jp.co.sony.csl.dcoes.apis.tools.web.deprecated;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.VertxConfig;
import jp.co.sony.csl.dcoes.apis.tools.web.api_handler.DealGeneration;

/**
 * 外部から融通情報を投入するための Web API を提供する Verticle.
 * すでに {@link jp.co.sony.csl.dcoes.apis.tools.web.ApiServer.ApiHandler} として提供しているため {@code @Deprecated}.
 * 処理は {@link DealGeneration} に移譲ずみ.
 * 以下の API を提供する.
 * - /deal : 融通を投入する
 * @author OES Project
 */
@Deprecated public class DealGenerator extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(DealGenerator.class);

	/**
	 * サービスを開くポートのデフォルト値.
	 * 値は {@value}
	 */
	private static final int DEFAULT_PORT = 9998;

	private static final DealGeneration handler_ = new DealGeneration();

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

	private void startHttpService_(Handler<AsyncResult<Void>> completionHandler) {
		Integer port = VertxConfig.config.getInteger(DEFAULT_PORT, "dealGenerator", "port");
		vertx.createHttpServer().requestHandler(req -> {
			req.exceptionHandler(t -> {
				log.error("exceptionHandler : " + t);
				req.response().setChunked(true).putHeader("content-type", "text/plain").setStatusCode(500).end("exceptionHandler : " + t + '\n');
			});
			try {
				if (req.path().equals("/deal")) {
					handler_.handleRequest(vertx, req, log);
				} else {
					if (log.isWarnEnabled()) log.warn("not found : " + req.uri());
					req.response().setStatusCode(404).end();
				}
			} catch (Exception e) {
				log.error("exception : " + e);
				req.response().setChunked(true).putHeader("content-type", "text/plain").setStatusCode(500).end("exception : " + e + '\n');
			}
		}).listen(port, res -> {
			if (res.succeeded()) {
				if (log.isInfoEnabled()) log.info("deal generation http service started on port : " + port);
				completionHandler.handle(Future.succeededFuture());
			} else {
				log.error(res.cause());
				completionHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}

}
