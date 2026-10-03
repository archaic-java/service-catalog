package work.archaic.service.logging.v03;

import java.lang.ScopedValue;

/** Scoped lookup; no global provider installation and no ordinary thread inheritance. */
final class ContextBinding {
    private static final ScopedValue<Context> CURRENT = ScopedValue.newInstance();
    private ContextBinding() {}

    static Context currentOrNull() { return CURRENT.isBound() ? CURRENT.get() : null; }
    static <E extends Exception> void where(Context context, Work<E> work) throws E {
        ScopedValue.where(CURRENT, context).call(() -> { work.run(); return null; });
    }
}
