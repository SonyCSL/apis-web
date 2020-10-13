package jp.co.sony.csl.dcoes.apis.tools.web.api_handler;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.JsonObjectUtil;
import jp.co.sony.csl.dcoes.apis.tools.web.ApiServer;

/**
 * 外部から融通情報を投入するための Web API を提供する.
 * {@link ApiServer} で使用される.
 * 以下の API を提供する.
 * - /deal : 融通を投入する
 * @author OES Project
 */
public class DealGeneration implements ApiServer.ApiHandler {

	/**
	 * {@inheritDoc}
	 * パスが {@code "/deal"} で始まっていれば処理する.
	 */
	@Override public boolean canHandleRequest(HttpServerRequest req) {
		return (req.path().startsWith("/deal"));
	}

	/**
	 * {@inheritDoc}
	 * リクエストのメソッドが POST ならフォームの {@code "json"} 値から {@link JsonObject} を生成し融通登録サービスに投げる.
	 * リクエストのメソッドが GET なら入力フォームを出力する.
	 */
	@Override public void handleRequest(Vertx vertx, HttpServerRequest req, Logger log) {
		switch (req.method()) {
		case POST:
			req.setExpectMultipart(true);
			req.endHandler(v -> {
				try {
					String json = req.getFormAttribute("json");
					JsonObjectUtil.toJsonObject(json, res -> {
						if (res.succeeded()) {
							JsonObject deal = res.result();
							String msg = "requesting deal creation : " + deal + " ...";
							if (log.isInfoEnabled()) log.info(msg);
							vertx.eventBus().send(ServiceAddress.Mediator.dealCreation(), deal);
							req.response().setChunked(true).putHeader("content-type", "text/plain").end(msg + '\n');
						} else {
							String msg = "illegal json format : " + res.cause();
							if (log.isWarnEnabled()) log.warn(msg);
							req.response().setChunked(true).putHeader("content-type", "text/plain").setStatusCode(500).end(msg + '\n');
						}
					});
				} catch (Exception e) {
					log.error("exception : " + e);
					req.response().setChunked(true).putHeader("content-type", "text/plain").setStatusCode(500).end("exception : " + e + '\n');
				}
			});
			break;
		case GET:
			req.response().setChunked(true).putHeader("content-type", "text/html").write(""
					+ "<html>"
					+ "<head></head>"
					+ "<body>"
					+ "<form method=\"POST\" action=\"/deal\">"
					+ "<textarea name=\"json\" rows=\"4\" cols=\"50\" placeholder=\"Deal JSON\"></textarea>"
					+ "<input type=\"submit\" value=\"Generate\">"
					+ "</form>"
					+ "</body>"
					+ "</html>"
					).end();
			break;
		default:
			if (log.isWarnEnabled()) log.warn("method " + req.method() + " not allowed");
			req.response().setStatusCode(405).end();
			break;
		}
	}

}
