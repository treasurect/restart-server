package com.treasure.restart.func.ai;

import com.treasure.restart.enums.AiMessageStatus;
import com.treasure.restart.helper.LogHelper;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AiStreamContext {
    private final AtomicReference<AiMessageStatus> status = new AtomicReference<>(AiMessageStatus.GENERATING);
    //    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final StringBuilder content = new StringBuilder();
    private volatile Disposable subscription;

    public void setSubscription(Disposable subscription) {
        this.subscription = subscription;
        if (status.get() != AiMessageStatus.GENERATING && subscription != null) {
            subscription.dispose();
        }
    }

    private void disposeSubscription() {
        Disposable current = subscription;
        if (current != null && !current.isDisposed()) {
            current.dispose();
        }
    }

//    public void cancel() {
//        LogHelper.info("AiStreamContext -> cancel -> pre status:" + status.get());

    /// /        if (cancelled.compareAndSet(false, true)) {
//        if (status.compareAndSet(AiMessageStatus.GENERATING, AiMessageStatus.CANCELLED)) {
//            Disposable current = subscription;
//            if (current != null) current.dispose();
//        }
//    }
    public void cancel() {
        transitionTo(AiMessageStatus.CANCELLED);
    }

    public void complete() {
        transitionTo(AiMessageStatus.COMPLETED);
    }

    public void fail() {
        transitionTo(AiMessageStatus.FAILED);
    }

    public void timeout() {
        transitionTo(AiMessageStatus.TIMEOUT);
    }

    private void transitionTo(AiMessageStatus target) {
        boolean changed = status.compareAndSet(AiMessageStatus.GENERATING, target);

        LogHelper.info("AiStreamContext -> transition, target=" + target + ", changed=" + changed + ", current=" + status.get());

        // 只有异常终止状态才取消上游订阅
        if (changed && target != AiMessageStatus.COMPLETED) {
            disposeSubscription();
        }
    }

    public boolean isGenerating() {
        return status.get() == AiMessageStatus.GENERATING;
    }

    public boolean isCancelled() {
        return status.get() == AiMessageStatus.CANCELLED;
    }

    public boolean isComplete() {
        return status.get() == AiMessageStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status.get() == AiMessageStatus.FAILED;
    }

    public boolean isTimeout() {
        return status.get() == AiMessageStatus.TIMEOUT;
    }

    public AiMessageStatus getStatus() {
        return status.get();
    }

//    public boolean isCancelled() {
//        return cancelled.get();
//    }

    public synchronized void append(String message) {
        if (message != null) content.append(message);
    }

    public synchronized String getContent() {
        return content.toString();
    }
}
