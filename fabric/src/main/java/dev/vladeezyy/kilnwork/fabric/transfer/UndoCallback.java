package dev.vladeezyy.kilnwork.fabric.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Undoes a change that was applied at once when its transaction (or an outer one) is aborted.
 */
public final class UndoCallback implements TransactionContext.CloseCallback {

    private final Runnable undo;

    private UndoCallback(Runnable undo) {
        this.undo = undo;
    }

    public static void register(TransactionContext transaction, Runnable undo) {
        transaction.addCloseCallback(new UndoCallback(undo));
    }

    @Override
    public void onClose(TransactionContext transaction, TransactionContext.Result result) {
        if (result.wasAborted()) {
            undo.run();
        } else if (transaction.nestingDepth() > 0) {
            transaction.getOpenTransaction(transaction.nestingDepth() - 1).addCloseCallback(this);
        }
    }
}
