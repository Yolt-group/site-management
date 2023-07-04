package nl.ing.lovebird.sitemanagement.lib.futures;

import lombok.NonNull;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.CompletableFuture;

public class Futures {

    public static <T> CompletableFuture<T> from(final ListenableFuture<T> listenableFuture) {
        CompletableFuture<T> completable = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(mayInterruptIfRunning);
                return result;
            }
        };

        listenableFuture.addCallback(new ListenableFutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completable.complete(result);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                completable.completeExceptionally(t);
            }
        });
        return completable;
    }

}
