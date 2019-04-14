package com.lavacablasa.ladc.core;

public interface GameContext {
    GfxOutput getGfxOutput();
    InputPlugin getInput();
    byte[] load(String resource);
}
