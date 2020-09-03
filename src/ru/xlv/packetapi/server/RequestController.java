package ru.xlv.packetapi.server;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Инструмент для управления обработкой запросов.
 * <p>
 * Следует использовать для отклонения выполнения нежелательных пакетов при определенных условиях.
 * <p>
 * - {@link Limited} позволит отсекать запросы, если их общее количество в очереди превышает допустимый уровень.
 * <p>
 * - {@link Periodic} позволит отсекать запросы, если их частота превышает допустимый период.
 * */
public abstract class RequestController<T> {

    private static final List<RequestController<Object>> LISTENING_CONTROLLERS = new ArrayList<>();
    private static Thread THREAD;

    protected final Map<T, Request> requestMap = new HashMap<>();

    private RequestController() {}

    /**
     * Позволяет исполнить код, если это допускается контроллером.
     * @param key ключ запроса.
     * @param runnable будет выполнен в случае успешной проверки контроллером.
     * */
    public boolean doRequestSync(T key, Runnable runnable) {
        if(!tryRequest(key)) return false;
        runnable.run();
        return true;
    }

    /**
     * Позволяет исполнить код асинхронно, если это допускается контроллером.
     * @param key ключ запроса.
     * @param runnable будет выполнен в случае успешной проверки контроллером.
     * */
    public void doCompletedRequestAsync(T key, Runnable runnable) {
        if (!tryRequest(key)) {
            synchronized (RequestController.class) {
                Request value;
                if (!requestMap.containsKey(key)) {
                    value = createRequest(key, runnable);
                    requestMap.put(key, value);
                } else {
                    value = requestMap.get(key);
                }
                updateRequest(value, runnable);
                startListeningController(this);
            }
        } else {
            runnable.run();
        }
    }

    /**
     * @return true, если запрос с входным ключом может быть совершен в данный момент.
     * */
    public boolean tryRequest(T key) {
        Request request;
        synchronized (RequestController.class) {
            request = requestMap.get(key);
        }
        if (request == null) {
            synchronized (RequestController.class) {
                requestMap.put(key, request = createRequest(key, null));
            }
            updateRequest(request, null);
            return true;
        } else {
            boolean b = canRequest(request);
            updateRequest(request, null);
            return b;
        }
    }

    protected abstract void updateRequest(Request request, Runnable runnable);

    protected abstract boolean canRequest(Request request);

    protected abstract void handleRequests();

    protected abstract Request createRequest(T key, Runnable runnable);

    private static <T> void startListeningController(RequestController<T> requestController) {
        synchronized (RequestController.class) {
            if (!LISTENING_CONTROLLERS.contains(requestController)) {
                //noinspection unchecked
                LISTENING_CONTROLLERS.add((RequestController<Object>) requestController);
            }
        }
        if (THREAD == null || !THREAD.isAlive()) {
            THREAD = new Thread(() -> {
                try {
                    do {
                        Iterator<RequestController<Object>> iterator = LISTENING_CONTROLLERS.iterator();
                        while (iterator.hasNext()) {
                            RequestController<Object> next = iterator.next();
                            next.handleRequests();
                            if (next.requestMap.isEmpty()) {
                                iterator.remove();
                            }
                        }
                    } while (!LISTENING_CONTROLLERS.isEmpty());
                } catch (Exception e) {
                    e.printStackTrace();
                    LISTENING_CONTROLLERS.clear();
                }
            });
            THREAD.start();
        }
    }

    /**
     * Данный контроллер позволяет отсекать запросы, если их общее количество в очереди превышает допустимый уровень.
     * */
    public static class Limited<T> extends RequestController<T> {

        protected int requestLimit = 1000;

        public Limited() {}

        public Limited(int requestLimit) {
            this.requestLimit = requestLimit;
        }

        @Override
        protected void updateRequest(Request request, Runnable runnable) {
            ((CountedRequest) request).requestCounter.incrementAndGet();
        }

        @Override
        protected boolean canRequest(Request request) {
            return ((CountedRequest) request).requestCounter.get() < requestLimit;
        }

        @Override
        protected void handleRequests() {
            Iterator<Map.Entry<T, Request>> iterator = requestMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<T, Request> next = iterator.next();
                Request request = next.getValue();
                if(canRequest(request) && ((CountedRequest) request).requestCounter.get() <= 1) {
                    try {
                        if (request.runnable != null) {
                            request.runnable.run();
                            ((CountedRequest) request).requestCounter.decrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    iterator.remove();
                }
            }
        }

        @Override
        protected Request createRequest(T key, Runnable runnable) {
            return new CountedRequest(runnable);
        }
    }

    /**
     * Данный контроллер позволяет отсекать запросы, если их частота превышает допустимый период.
     * */
    public static class Periodic<T> extends RequestController<T> {

        /**
         * Указывается в миллисекундах
         * */
        protected long requestPeriod = 1000L;

        public Periodic() {}

        public Periodic(long requestPeriod) {
            this.requestPeriod = requestPeriod;
        }

        @Override
        protected void updateRequest(Request request, Runnable runnable) {
            request.requestTimeMills = System.currentTimeMillis() + requestPeriod;
            if(runnable != null) {
                ((MultiRequest) request).runnableQueue.add(runnable);
            }
        }

        @Override
        protected boolean canRequest(Request request) {
            return request.requestTimeMills < System.currentTimeMillis();
        }

        @Override
        protected void handleRequests() {
            Iterator<Request> iterator = requestMap.values().iterator();
            while (iterator.hasNext()) {
                MultiRequest multiRequest = (MultiRequest) iterator.next();
                if(canRequest(multiRequest)) {
                    try {
                        if (!multiRequest.runnableQueue.isEmpty()) {
                            System.out.println(multiRequest.runnableQueue.size());
                            Runnable poll = multiRequest.runnableQueue.poll();
                            if (poll != null) {
                                poll.run();
                            } else {
                                continue;
                            }
                        }
                        if(multiRequest.runnableQueue.isEmpty()) {
                            iterator.remove();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected MultiRequest createRequest(T key, Runnable runnable) {
            return new MultiRequest(runnable);
        }
    }

    private static class MultiRequest extends Request {

        private final Queue<Runnable> runnableQueue = new LinkedList<>();

        public MultiRequest(Runnable runnable) {
            super(null);
            if(runnable != null) {
                runnableQueue.add(runnable);
            }
        }
    }

    private static class CountedRequest extends Request {

        private final AtomicInteger requestCounter = new AtomicInteger(0);

        public CountedRequest(Runnable runnable) {
            super(runnable);
        }
    }

    private static class Request {
        private long requestTimeMills;
        private final Runnable runnable;

        public Request(Runnable runnable) {
            this.runnable = runnable;
        }
    }
}
