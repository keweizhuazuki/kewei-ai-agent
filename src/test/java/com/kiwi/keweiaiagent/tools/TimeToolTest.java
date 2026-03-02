package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeToolTest {

    @Test
    void shouldReturnCurrentDateTimeWithDefaultZone() {
        TimeTool tool = new TimeTool();
        String result = tool.getCurrentDateTime(null);
        Assertions.assertTrue(result.contains("zone: "));
        Assertions.assertTrue(result.contains("date: "));
        Assertions.assertTrue(result.contains("time: "));
        Assertions.assertTrue(result.contains("datetime: "));
        Assertions.assertTrue(result.contains("epochMillis: "));
    }

    @Test
    void shouldReturnErrorWhenZoneInvalid() {
        TimeTool tool = new TimeTool();
        String result = tool.getCurrentDateTime("invalid/timezone");
        Assertions.assertTrue(result.contains("invalid timezone"));
    }
}
