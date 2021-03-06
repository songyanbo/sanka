package sanka;

import java.util.Stack;
import java.util.TreeMap;

public class SymbolTable {

    static class Frame extends TreeMap<String, TypeDefinition> {
        private static final long serialVersionUID = 1;
    }

    private Stack<Frame> stack;

    SymbolTable() {
        this.stack = new Stack<>();
    }

    Frame push(Frame frame) {
        if (frame == null) {
            frame = new Frame();
        }
        this.stack.push(frame);
        return frame;
    }

    Frame pop() {
        return this.stack.pop();
    }

    void put(String name, TypeDefinition type) {
        this.stack.peek().put(name, type);
    }

    TypeDefinition get(String name) {
        for (int idx = this.stack.size()-1; idx >= 0; idx--) {
            Frame frame = this.stack.get(idx);
            TypeDefinition value = frame.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    void promote(String name, TypeDefinition type) {
        for (int idx = this.stack.size()-1; idx >= 0; idx--) {
            Frame frame = this.stack.get(idx);
            TypeDefinition value = frame.get(name);
            if (value != null) {
                frame.put(name,  type);
                return;
            }
        }
    }
}
