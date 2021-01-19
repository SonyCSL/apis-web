package jp.co.sony.csl.dcoes.apis.tools.web.api_handler;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.tools.web.ApiServer;

/**
 * Provides Web API to change the level of the log from the outside. 
 * Used in {@link ApiServer}.
 * The following API is provided.
 * - /log : Changes the the log level for all programs participating in the cluster.
 * @author OES Project
 * 外部からログのレベルを変更するための Web API を提供する.
 * {@link ApiServer} で使用される.
 * 以下の API を提供する.
 * - /log : クラスタに参加する全プログラムに対しログのレベルを変更する
 * @author OES Project
*/
public class LogConfiguration implements ApiServer.ApiHandler {

	/**
	 * {@inheritDoc}
	 * Processes if path begins with {@code "/log"}.
	 * {@inheritDoc}
	 * パスが {@code "/log"} で始まっていれば処理する.
	 */
	@Override public boolean canHandleRequest(HttpServerRequest req) {
		return (req.path().startsWith("/log"));
	}

	/**
	 * {@inheritDoc}
	 * If the request method is POST, delivers to the log level change service under the following conditions.
	 * - handler : Log handler name.
	 *             At present, only {@code "Multicast"} is supported.
	 * - level : Log level.
	 *           If unspecified or empty, reverts to the level at startup.
	 * If the request method is GET, outputs the input form.
	 * {@inheritDoc}
	 * リクエストのメソッドが POST なら以下の条件でログレベル変更サービスに投げる.
	 * - handler : ログハンドラ名.
	 *             現状は {@code "Multicast"} のみ対応.
	 * - level : ログレベル.
	 *           指定なしまたは空文字なら起動時のレベルに戻す.
	 * リクエストのメソッドが GET なら入力フォームを出力する.
	 */
	@Override public void handleRequest(Vertx vertx, HttpServerRequest req, Logger log) {
		switch (req.method()) {
		case POST:
			req.setExpectMultipart(true);
			req.endHandler(v -> {
				try {
					String handler = req.getFormAttribute("handler");
					String level = req.getFormAttribute("level");
					if ("Multicast".equals(handler)) {
						String msg = "publishing new multicast log level : " + level + " ...";
						if (log.isInfoEnabled()) log.info(msg);
						vertx.eventBus().publish(ServiceAddress.multicastLogHandlerLevel(), level);
						req.response().setChunked(true).putHeader("content-type", "text/plain").end(msg + '\n');
					} else {
						String msg = "unknown log handler : " + handler;
						if (log.isWarnEnabled()) log.warn(msg);
						req.response().setChunked(true).putHeader("content-type", "text/plain").setStatusCode(500).end(msg + '\n');
					}
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
					+ "<form method=\"POST\" action=\"/log\">"
					+ "<select name=\"handler\">"
					+ "<option>Multicast</option>"
					+ "</select>"
					+ "<select name=\"level\"><option></option>"
					+ "<option>OFF</option>"
					+ "<option>SEVERE</option>"
					+ "<option>WARNING</option>"
					+ "<option>INFO</option>"
					+ "<option>CONFIG</option>"
					+ "<option>FINE</option>"
					+ "<option>FINER</option>"
					+ "<option>FINEST</option>"
					+ "<option>ALL</option>"
					+ "</select>"
					+ "<input type=\"submit\" value=\"Set\">"
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
