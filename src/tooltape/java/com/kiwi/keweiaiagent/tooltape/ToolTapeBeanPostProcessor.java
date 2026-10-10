package com.kiwi.keweiaiagent.tooltape;

import dev.tooltape.CapturePolicy;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Opt-in interception of the two callback arrays exported by ToolRegistration. */
public final class ToolTapeBeanPostProcessor implements BeanPostProcessor, DisposableBean {
    private static final Set<String> TARGET_BEANS = Set.of("allTools", "memoryTools");
    private final ToolTapeSession session;

    public ToolTapeBeanPostProcessor(Environment environment) throws IOException {
        String mode = environment.getProperty("kewei.tooltape.mode", "off");
        if (mode.equals("off")) {
            session = null;
            return;
        }
        if (!mode.equals("record") && !mode.equals("replay")) {
            throw new IllegalArgumentException("kewei.tooltape.mode must be off, record, or replay");
        }
        String path = environment.getProperty("kewei.tooltape.path");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("kewei.tooltape.path is required for record/replay");
        }
        Set<String> inputFields = Arrays.stream(environment
                        .getProperty("kewei.tooltape.input-fields", "").split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toSet());
        // This is a separate explicit privacy opt-in: arbitrary tool text is not sanitizable.
        boolean captureOutput = environment.getProperty(
                "kewei.tooltape.capture-output", Boolean.class, false);
        CapturePolicy policy = CapturePolicy.allow(inputFields,
                captureOutput ? Set.of(ToolTapeSession.RESULT_FIELD) : Set.of());
        session = mode.equals("record")
                ? ToolTapeSession.record(Path.of(path), policy)
                : ToolTapeSession.replay(Path.of(path), policy);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (session == null || !TARGET_BEANS.contains(beanName)) {
            return bean;
        }
        if (!(bean instanceof ToolCallback[] callbacks)) {
            throw new IllegalStateException("ToolTape target must be a ToolCallback array: " + beanName);
        }
        return Arrays.stream(callbacks).map(session::wrap).toArray(ToolCallback[]::new);
    }

    @Override
    public void destroy() throws IOException {
        if (session != null) {
            session.close();
        }
    }
}
