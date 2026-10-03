package work.archaic.service.logging.v03;

import java.lang.ScopedValue;

/** Scoped lookup; no global provider installation and no ordinary thread inheritance. */
final class ContextBinding {
    private static final ScopedValue<Context> CURRENT = ScopedValue.newInstance();
    private ContextBinding() {}

    static Context currentOrNull() { return CURRENT.isBound() ? CURRENT.get() : null; }
    static <T, E extends Exception> T where(Context context, Call<T, E> work) throws E {
        return ScopedValue.where(CURRENT, context).call(work::call);
    }
}
