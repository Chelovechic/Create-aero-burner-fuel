package com.create_aeronauticsad.burnefuelmod;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue BURN_RATE_MULTIPLIER = BUILDER
            .comment("multiplier")
            .defineInRange("burnRateMultiplier", 1.0D, 0.001D, Double.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
