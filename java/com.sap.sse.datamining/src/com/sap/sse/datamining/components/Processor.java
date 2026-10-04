package com.sap.sse.datamining.components;

public interface Processor<InputType, ResultType> {
    
    /**
     * @return <code>true</code>, if the processor accepts new elements to process.
     */
    public boolean canProcessElements();

    /**
     * Processes the given element and forwards the result.
     * @param element The element to process.
     */
    public void processElement(InputType element);

    /**
     * This method handles the throwing of Throwables, so they don't get lost because of the 
     * multi-threading.<br>
     * The standard implementation is forwarding them to the last processor,
     * that collects the failures, until the processing is finished. Than the failures will be
     * handled.
     * @param failure The thrown failure.
     */
    void onFailure(Throwable failure);

    /**
     * Tells this Processor, that there won't be any input anymore. This has the effect, that
     * this Processor won't process new input ({@link #canProcessElements()} will return <code>false</code>
     * after this method has been called).<br>
     * The called Processor will finish its work and call <code>finish()</code> on all subsequent processors.
     * 
     * @throws InterruptedException
     */
    public void finish() throws InterruptedException;
    public boolean isFinished();

    /**
     * Aborts the processing immediately. The result will be <code>null</code>, incomplete or undefined.<br />
     * To shut down the process cleanly use {@link #finish()}.
     */
    public void abort();
    public boolean isAborted();
    
    public Class<InputType> getInputType();
    
    public Class<ResultType> getResultType();

    /**
     * Takes a result builder and fills it with its additional data and the data of its result receivers.
     * @return The builder filled with the additional data of all processors in the chain. It can be used to
     *         construct the additional data of the executed processor chain. 
     */
    public AdditionalResultDataBuilder getAdditionalResultData(AdditionalResultDataBuilder additionalDataBuilder);

    /**
     * Enqueues a callback for the event where this processor has finished processing what it has been
     * provided so far through calls to {@link #processElement(Object)}. Subclasses, especially those
     * working with result receivers and thread pools for parallel processing need to check their dependent
     * processors for having finished as well before invoking the callback.<p>
     * 
     * This default implementation immediately invokes the callback, assuming that {@link #processElement(Object)}
     * is a synchronous method that does not spawn any background processing.
     * 
     * @param callbackWhenAllLoadedFixesHaveBeenProcessed must not be {@code null}
     */
    default void runWhenFinishedProcessing(Runnable callbackWhenAllLoadedFixesHaveBeenProcessed) {
        callbackWhenAllLoadedFixesHaveBeenProcessed.run();
    }

}
