package org.example;

import java.util.regex.*;

public class CodeExtractor {
    /* 抓取 ```lang ... ``` 里的代码块，返回 [0]=剩余文字 [1]=代码（无围栏） */
    public static String[] split(String resp) {
        Pattern p = Pattern.compile("```(\\w+)?\\s*\\n(.*?)\\n```", Pattern.DOTALL);
        Matcher m = p.matcher(resp);
        if (!m.find()) return new String[]{resp, null};          // 没代码
        String code = m.group(2);
        String text = resp.substring(0, m.start()) + resp.substring(m.end());
        return new String[]{text.trim(), code.trim()};
    }
}