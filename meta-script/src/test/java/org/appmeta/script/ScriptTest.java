package org.appmeta.script;
/*
 * @project app-meta-server
 * @file    org.appmeta.script.ScriptTest
 * CREATE   2023年12月20日 13:27 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import javax.script.*;
import java.util.List;

public class ScriptTest {

    @Test
    public void engineList(){
        List<ScriptEngineFactory> engines = new ScriptEngineManager().getEngineFactories();
        for (ScriptEngineFactory f : engines) {
            System.out.println(f.getLanguageName() + " " + f.getEngineName() + " " + f.getNames());
        }
    }

    @Test
    public void run() throws ScriptException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("engine.WarnInterpreterOnly", false);
        bindings.put("x", 100);

        engine.eval("console.log('Hello World!');");

        CompiledScript script = ((Compilable) engine).compile("1E6 + x");
        System.out.println(script.eval(bindings));
    }

    /**
     * 配置上下文并执行 JS
     */
    @Test
    public void run2(){
        Engine engine = Engine.newBuilder()
//                .allowExperimentalOptions(true)
                .option("engine.WarnInterpreterOnly", "false")
//                .option("js.console", "false")
                .build();
        Context ctx = Context.newBuilder("js")
                //设置 JS 与 JAVA 的交互性（如 Java.type、Packages ）
//                .allowAllAccess(true)
//                .allowIO(false)
                //设置为 HostAccess.ALL 后，可以在 js 中调用 java 方法（通过 Bindings 传递），但是不支持使用 Java.type 功能
                .allowHostAccess(HostAccess.ALL)
                .engine(engine).build();

        Value ctxValue = ctx.getBindings("js");
        ctxValue.putMember("name", "集成显卡");
        ctxValue.putMember("phone", new Phone("18000000000"));

        Object result = ctx.eval("js", """
                if (typeof Graal != 'undefined') {
                    print("Graal.versionECMAScript", "\t=", Graal.versionECMAScript);
                    print("Graal.versionGraalVM", "\t\t=",Graal.versionGraalVM);
                    print("Graal.isGraalRuntime", "\t\t=",Graal.isGraalRuntime());
                }
                console.log(name, JSON.stringify(phone))
                phone.call('hello')
                let { mime, date } = phone.mime()
                console.log(`从 JAVA 中获取的 MIME=`, mime);
                
                `date=${date}`
                """);
        System.out.println(result);
    }
}
