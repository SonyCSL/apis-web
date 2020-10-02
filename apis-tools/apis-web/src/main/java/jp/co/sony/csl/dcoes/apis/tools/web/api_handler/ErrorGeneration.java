package jp.co.sony.csl.dcoes.apis.tools.web.api_handler;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import jp.co.sony.csl.dcoes.apis.common.Error;
import jp.co.sony.csl.dcoes.apis.tools.web.ApiServer;

/**
 * 外部からエラーを投入するための Web API を提供する.
 * {@link ApiServer} で使用される.
 * 以下の API を提供する.
 * - /error : 意図的にエラーを発生させる
 * @author OES Project
 */
public class ErrorGeneration implements ApiServer.ApiHandler {

	/**
	 * {@inheritDoc}
	 * パスが {@code "/error"} で始まっていれば処理する.
	 */
	@Override public boolean canHandleRequest(HttpServerRequest req) {
		return (req.path().startsWith("/error"));
	}

	/**
	 * {@inheritDoc}
	 * リクエストのメソッドが POST なら以下の属性でエラーを生成し投げる.
	 * - unitId : エラー生成ユニット ID
	 * - category : エラーの category
	 * - extent : エラーの extent
	 * - level : エラーの level
	 * - message : エラーメッセージ
	 * リクエストのメソッドが GET なら入力フォームを出力する.
	 */
	@Override public void handleRequest(Vertx vertx, HttpServerRequest req, Logger log) {
		switch (req.method()) {
		case POST:
			req.setExpectMultipart(true);
			req.endHandler(v -> {
				try {
					String unitId = req.getFormAttribute("unitId");
					Error.Category category = Error.category(req.getFormAttribute("category").toUpperCase());
					Error.Extent extent = Error.extent(req.getFormAttribute("extent").toUpperCase());
					Error.Level level = Error.level(req.getFormAttribute("level").toUpperCase());
					String message = req.getFormAttribute("message");
					String msg = "publishing error : " + Error.logMessage(category, extent, level, message, unitId, null) + " ...";
					if (log.isInfoEnabled()) log.info(msg);
					Error.report(vertx, unitId, category, extent, level, message);
					req.response().setChunked(true).putHeader("content-type", "text/plain").end(msg + '\n');
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
					+ "<form method=\"POST\" action=\"/error\">"
					+ "<input type=\"text\" name=\"unitId\" placeholder=\"Unit ID\">"
					+ "<select name=\"category\"><option></option>"
					+ "<option>USER</option>"
					+ "<option>HARDWARE</option>"
					+ "<option>LOGIC</option>"
					+ "<option>FRAMEWORK</option>"
					+ "</select>"
					+ "<select name=\"extent\"><option></option>"
					+ "<option>LOCAL</option>"
					+ "<option>GLOBAL</option>"
					+ "</select>"
					+ "<select name=\"level\"><option></option>"
					+ "<option>WARN</option>"
					+ "<option>ERROR</option>"
					+ "<option>FATAL</option>"
					+ "</select>"
					+ "<input type=\"text\" name=\"message\" placeholder=\"Message\"></textarea>"
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
