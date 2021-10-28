package dc.local.electriccar;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

/*
  ExampleStaticHandler.java found on GitHubGist written by yeoupooh
*/
class IStaticHandlerFactory {

    static StaticHandler create(IStaticHandler ref) {
        return new StaticHandler(ref);
    }

    // This has to be nested.
    private static class StaticHandler extends Handler {
        final WeakReference<IStaticHandler> weakRef;

        @SuppressWarnings("deprecation")
        StaticHandler(IStaticHandler ref) {
            this.weakRef = new WeakReference<>(ref);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (weakRef.get() == null) {
                throw new RuntimeException("Something goes wrong.");
            } else {
                weakRef.get().handleMessage(msg);
            }
        }
    }
}