package com.lavacablasa.ladc.gwt;

import static elemental2.dom.DomGlobal.document;

import com.google.gwt.core.client.EntryPoint;
import com.lavacablasa.ladc.abadia.CPC6128;
import com.lavacablasa.ladc.abadia.Juego;
import com.lavacablasa.ladc.core.GameContext;
import com.lavacablasa.ladc.core.Input;
import elemental2.dom.CanvasRenderingContext2D;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.ImageData;
import elemental2.dom.KeyboardEvent;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import org.jboss.gwt.elemento.core.EventType;
import org.jboss.gwt.elemento.core.Key;

public class GwtGameContext extends GameContext implements EntryPoint {
    public static final int WIDTH = 320;
    public static final int HEIGHT = 200;
    private final HTMLCanvasElement canvas;
    private final CanvasRenderingContext2D c2d;
    private final double colors[][] = new double[32][3];
    private final Set<Input> pressedInputs = EnumSet.noneOf(Input.class);
    private final ImageData buffer;

    public GwtGameContext() {
        var container = document.createElement("div");
        container.classList.add("container");
        document.body.appendChild(container);

        canvas = Js.cast(document.createElement("canvas"));
        canvas.width = WIDTH; canvas.height = HEIGHT;
        c2d = Js.cast(canvas.getContext("2d"));
        buffer = c2d.createImageData(WIDTH, HEIGHT);
        container.appendChild(canvas);

        EventType.bind(document, EventType.keydown, ev -> {
            var input = asKey(ev);
            if (input != null) pressedInputs.add(input);
        });
        EventType.bind(document, EventType.keyup, ev -> {
            var input = asKey(ev);
            if (input != null) pressedInputs.remove(input);
        });
    }
    static @Nullable Input asKey(KeyboardEvent ev) {
        switch (Key.fromEvent(ev)) {
            case Delete: return Input.SUPR;
            case Enter: return Input.INTRO;
            case Spacebar: return Input.SPACE;
            case ArrowUp: return Input.UP;
            case ArrowDown: return Input.DOWN;
            case ArrowLeft: return Input.LEFT;
            case ArrowRight: return Input.RIGHT;
        }
        if ("s".equalsIgnoreCase(ev.key)) return Input.S;
        if ("n".equalsIgnoreCase(ev.key)) return Input.N;
        if ("q".equalsIgnoreCase(ev.key)) return Input.Q;
        if ("r".equalsIgnoreCase(ev.key)) return Input.R;
        return null;
    }

    @Override public void onModuleLoad() {
        Juego juego = new Juego(readDiskImageToMemory(AbadiaDsk.load()), new CPC6128(this), this);
        juego.gameLogicLoop();
        juego.mainSyncLoop();
    }

    @Override public void render() {
        c2d.putImageData(buffer, 0, 0);
    }

    @Override public void setPixel(int x, int y, int color) {
        var red = y * (WIDTH * 4) + x * 4;
        buffer.data.setAt(red + 0, colors[color][0]);
        buffer.data.setAt(red + 1, colors[color][1]);
        buffer.data.setAt(red + 2, colors[color][2]);
        buffer.data.setAt(red + 3, 255.);
    }

    @Override public void setColor(int color, byte r, byte g, byte b) {
        colors[color][0] = (((int) r) & 0xff);
        colors[color][1] = (((int) g) & 0xff);
        colors[color][2] = (((int) b) & 0xff);
    }

    @Override public void process(int[] inputs) {
        for (Input input : Input.values()) inputs[input.ordinal()] = pressedInputs.contains(input) ? 1 : 0;
    }

    @Override protected BiConsumer<Runnable, Integer> getTimer() {
        return (fn, ms) -> DomGlobal.setTimeout(p0 -> fn.run(), ms);
    }
}
