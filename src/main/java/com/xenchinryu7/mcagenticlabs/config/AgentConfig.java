package com.xenchinryu7.mcagenticlabs.config;

/**
 * Simple configuration class for Agent AI Mod
 * TODO: Replace with proper Fabric config library (e.g., Cloth Config) for GUI configuration
 */
public class AgentConfig {
    // AI API Configuration
    public static String AI_PROVIDER_VALUE = "groq"; // groq (FASTEST, FREE), openai, or gemini
    public static String OPENAI_API_KEY_VALUE = "";
    public static String OPENAI_MODEL_VALUE = "gpt-4-turbo-preview";
    public static int MAX_TOKENS_VALUE = 8000;
    public static double TEMPERATURE_VALUE = 0.7;
    
    // Agent Behavior Configuration
    public static int ACTION_TICK_DELAY_VALUE = 20;
    public static boolean ENABLE_CHAT_RESPONSES_VALUE = true;
    public static int MAX_ACTIVE_AGENTS_VALUE = 10;
    
    // Wrapper classes for backward compatibility
    public static class ConfigValue<T> {
        private final T value;
        public ConfigValue(T value) { this.value = value; }
        public T get() { return value; }
    }
    
    public static class IntValue extends ConfigValue<Integer> {
        public IntValue(Integer value) { super(value); }
    }
    
    public static class DoubleValue extends ConfigValue<Double> {
        public DoubleValue(Double value) { super(value); }
    }
    
    public static class BooleanValue extends ConfigValue<Boolean> {
        public BooleanValue(Boolean value) { super(value); }
    }
    
    // Static config values with getters for backward compatibility
    public static final ConfigValue<String> AI_PROVIDER = new ConfigValue<>(AI_PROVIDER_VALUE);
    public static final ConfigValue<String> OPENAI_API_KEY = new ConfigValue<>(OPENAI_API_KEY_VALUE);
    public static final ConfigValue<String> OPENAI_MODEL = new ConfigValue<>(OPENAI_MODEL_VALUE);
    public static final IntValue MAX_TOKENS = new IntValue(MAX_TOKENS_VALUE);
    public static final DoubleValue TEMPERATURE = new DoubleValue(TEMPERATURE_VALUE);
    public static final IntValue ACTION_TICK_DELAY = new IntValue(ACTION_TICK_DELAY_VALUE);
    public static final BooleanValue ENABLE_CHAT_RESPONSES = new BooleanValue(ENABLE_CHAT_RESPONSES_VALUE);
    public static final IntValue MAX_ACTIVE_AGENTS = new IntValue(MAX_ACTIVE_AGENTS_VALUE);
}
