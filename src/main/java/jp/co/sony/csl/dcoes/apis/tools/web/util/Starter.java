package jp.co.sony.csl.dcoes.apis.tools.web.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import jp.co.sony.csl.dcoes.apis.common.util.vertx.AbstractStarter;
import jp.co.sony.csl.dcoes.apis.tools.web.ApiServer;
import jp.co.sony.csl.dcoes.apis.tools.web.BudoEmulator;
import jp.co.sony.csl.dcoes.apis.tools.web.EmulatorEmulator;
import jp.co.sony.csl.dcoes.apis.tools.web.deprecated.DealGenerator;

/**
 * These are the main Verticles of apis-web.
 * Specified by maven-shade-plugin's {@literal <Main-Verticle>} in pom.xml.
 * Starts the following Verticles.
 * - {@link EmulatorEmulator} : Verticle that provides unit data to the outside. Emulates providing the unit data to main_controller in the DCDC emulator environment.
 * - {@link BudoEmulator} : Verticle that provides Power Sharing conditions, etc., to the outside. Emulates providing {@code BUDO} information to main_controller in the DCDC emulator environment.
 * - {@link ApiServer} : Verticle that provides various Web API for controlling APIS from the outside.
 * - {@link DealGenerator} : Verticle that provides various Web API for injecting Power Sharing information from the outside. {@code @Deprecated} because this is already provided as {@link ApiServer.ApiHandler} under {@link ApiServer}
 * @author OES Project
 * apis-web の親玉 Verticle.
 * pom.xml の maven-shade-plugin の {@literal <Main-Verticle>} で指定してある.
 * 以下の Verticle を起動する.
 * - {@link EmulatorEmulator} : 外部に対しユニットデータを提供する Verticle. DCDC emulator 環境での main_controller へのユニットデータ提供を模倣
 * - {@link BudoEmulator} : 外部に対し融通状況などを提供する Verticle. DCDC emulator 環境での main_controller への {@code BUDO} 情報提供を模倣
 * - {@link ApiServer} : 外部から APIS を制御するための各種 Web API を提供する Verticle
 * - {@link DealGenerator} : 外部から融通情報を投入するための Web API を提供する Verticle. すでに {@link ApiServer} 配下の {@link ApiServer.ApiHandler} として提供しているため {@code @Deprecated}
 * @author OES Project
 */
@SuppressWarnings("deprecation")
public class Starter extends AbstractStarter {

	/**
	 * Called from {@link AbstractStarter#start(Future)} during startup.
	 * 起動時に {@link AbstractStarter#start(Future)} から呼び出される.
	 */
	@Override protected void doStart(Handler<AsyncResult<Void>> completionHandler) {
		vertx.deployVerticle(new EmulatorEmulator(), resEmulatorEmulator -> {
			if (resEmulatorEmulator.succeeded()) {
				vertx.deployVerticle(new BudoEmulator(), resBudoEmulator -> {
					if (resBudoEmulator.succeeded()) {
						vertx.deployVerticle(new ApiServer(), resApiServer -> {
							if (resApiServer.succeeded()) {
								vertx.deployVerticle(new DealGenerator(), resDealGenerator -> {
									if (resDealGenerator.succeeded()) {
										completionHandler.handle(Future.succeededFuture());
									} else {
										completionHandler.handle(Future.failedFuture(resDealGenerator.cause()));
									}
								});
							} else {
								completionHandler.handle(Future.failedFuture(resApiServer.cause()));
							}
						});
					} else {
						completionHandler.handle(Future.failedFuture(resBudoEmulator.cause()));
					}
				});
			} else {
				completionHandler.handle(Future.failedFuture(resEmulatorEmulator.cause()));
			}
		});
	}

}
