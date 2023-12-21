package org.appmeta.script;
/*
 * @project app-meta-server
 * @file    org.appmeta.script.Phone
 * CREATE   2023年12月20日 15:11 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import java.util.HashMap;
import java.util.Map;

public class Phone {
    String number;

    public Phone(String number){
        this.number = number;
    }

    public void call(String target){
        System.out.println("[JAVA] 打给%s（本机号码 %s）".formatted(target, number));
    }

    public Map<String, Object> mime() throws InterruptedException {
        System.out.println("[JAVA] 等待2秒返回...");
        Thread.sleep(2000);
        return new HashMap<>() {{
            put("mime", number);
            put("date", System.currentTimeMillis());
        }};
    }
}
