package com.sap.sse.datamining.impl.components;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;

import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.components.ProcessorInstructionHandler;

public abstract class AbstractParallelProcessor<InputType, ResultType> extends AbstractProcessor<InputType, ResultType>
                                                                       implements ProcessorInstructionHandler<ResultType> {

    private static final Logger LOGGER = Logger.getLogger(AbstractParallelProcessor.class.getName());
    private static final int SLEEP_TIME_DURING_FINISHING = 100;

    private final Processor<ResultType, ?>[] resultReceivers;
    private final ExecutorService executor;
    private final AtomicInteger unfinishedInstructionsCounter;
    
    private boolean isFinished = false;
    private boolean isAborted = false;

    public AbstractParallelProcessor(Class<InputType> inputType, Class<ResultType> resultType, ExecutorService executor, Collection<Processor<ResultType, ?>> resultReceivers) {
        super(inputType, resultType);
        this.executor = executor;
        @SuppressWarnings("unchecked")
        final Processor<ResultType, ?>[] resultReceiversAsArray = (Processor<ResultType, ?>[]) new Processor<?, ?>[resultReceivers.size()];
        this.resultReceivers = resultReceivers.toArray(resultReceiversAsArray);
        unfinishedInstructionsCounter = new AtomicInteger();
    }
    
    @Override
    public boolean canProcessElements() {
        return !isFinished() && !isAborted();
    }

    @Override
    public void processElement(InputType element) {
        if (canProcessElements()) {
            final ProcessorInstruction<ResultType> instruction = createInstruction(element);
            if (isInstructionValid(instruction)) {
                unfinishedInstructionsCounter.getAndIncrement();
                try {
                    Runnable instructionToRun;
                    try {
                        final Subject subject = SecurityUtils.getSubject(); // pass on the current subject to the instruction
                        instructionToRun = subject == null ? instruction : subject.associateWith(instruction);
                    } catch (UnavailableSecurityManagerException e) {
                        instructionToRun = instruction;
                    }
                    executor.execute(instructionToRun);
                } catch (RejectedExecutionException exc) {
                    LOGGER.log(Level.FINEST, "A " + RejectedExecutionException.class.getSimpleName()
                            + " appeared during the processing.");
                    instruction.run();
                }
            }
        }
    }

    private boolean isInstructionValid(ProcessorInstruction<ResultType> instruction) {
        return instruction != null;
    }

    @Override
    public void instructionSucceeded(ResultType result) {
        forwardResultToReceivers(result);
    }

    @Override
    public void instructionFailed(Exception e) {
        if (!isAborted() || !(e instanceof InterruptedException)) {
            onFailure(e);
        }
    }

    @Override
    public void afterInstructionFinished(ProcessorInstruction<ResultType> instruction) {
        unfinishedInstructionsCounter.getAndDecrement();
    }
    
    protected Processor<ResultType, ?>[] getResultReceivers() {
        return resultReceivers;
    }
    
    /**
     * Forwards the given <code>result</code> to the result receivers, if it's {@link #isResultValid(Object) valid}
     * and if the processor hasn't been {@link #abort() aborted}.
     * 
     * @param result the element to forward to the result receivers
     */
    protected void forwardResultToReceivers(ResultType result) {
        if (isResultValid(result) && !isAborted()) {
            for (Processor<ResultType, ?> resultReceiver : resultReceivers) {
                resultReceiver.processElement(result);
            }
        }
    }

    /**
     * Checks if the given <code>result</code> valid. For example a valid element has to be not <code>null</code>.
     * If the element is valid, it can be forwarded to the result receivers.
     * 
     * @param result the element to check
     * @return <code>true</code>, if the result is valid, so it can be forwarded to the result receivers
     */
    private boolean isResultValid(ResultType result) {
        return result != null;
    }
    
    /**
     * @return An invalid result, that won't be forwarded to the result receivers.
     */
    protected ResultType createInvalidResult() {
        return null;
    }

    protected abstract ProcessorInstruction<ResultType> createInstruction(final InputType element);

    @Override
    public void onFailure(Throwable failure) {
        for (Processor<ResultType, ?> resultReceiver : resultReceivers) {
            resultReceiver.onFailure(failure);
        }
    }
    
    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void finish() throws InterruptedException {
        sleepUntilAllInstructionsFinished();
        if (!isAborted) {
            isFinished = true;
            tellResultReceiversToFinish();
        }
    }

    protected void sleepUntilAllInstructionsFinished() throws InterruptedException {
        while (areUnfinishedInstructionsLeft() && !isAborted) {
            try {
                Thread.sleep(SLEEP_TIME_DURING_FINISHING);
            } catch (InterruptedException e) {
                if (!isAborted) {
                    onFailure(e);
                }
            }
        }
    }

    private boolean areUnfinishedInstructionsLeft() {
        return unfinishedInstructionsCounter.get() > 0;
    }

    protected void tellResultReceiversToFinish() {
        for (Processor<ResultType, ?> resultReceiver : resultReceivers) {
            try {
                resultReceiver.finish();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, resultReceiver.toString() + " was interrupted", e);
            }
        }
    }

    @Override
    public boolean isAborted() {
        return isAborted;
    }
    
    @Override
    public void abort() {
        isAborted = true;
        tellResultReceiversToAbort();
        LOGGER.log(Level.INFO, "The processing got aborted.");
    }

    private void tellResultReceiversToAbort() {
        for (Processor<ResultType, ?> resultReceiver : resultReceivers) {
            resultReceiver.abort();
        }
    }
    
    @Override
    public AdditionalResultDataBuilder getAdditionalResultData(AdditionalResultDataBuilder additionalDataBuilder) {
        setAdditionalData(additionalDataBuilder);
        for (Processor<ResultType, ?> resultReceiver : resultReceivers) {
            additionalDataBuilder = resultReceiver.getAdditionalResultData(additionalDataBuilder);
        }
        return additionalDataBuilder;
    }
    
    protected abstract void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder);

}
