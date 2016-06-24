package com.netease.cns.agent.dstore;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by hzzhangdongya on 16-6-24.
 */
public class ObservablePathChildrenCache extends Observable<ChildData> {
    private static final Logger LOG = LoggerFactory.getLogger(ObservablePathChildrenCache.class);

    /**
     * Creates an Observable with a Function to execute when it is subscribed to.
     * <p>
     * <em>Note:</em> Use {@link #create(OnSubscribe)} to create an Observable, instead of this constructor,
     * unless you specifically have a need for inheritance.
     *
     * @param f {@link OnSubscribe} to be executed when {@link #subscribe(Subscriber)} is called
     */
    protected ObservablePathChildrenCache(OnSubscribe<ChildData> f) {
        super(f);
    }

    public static ObservablePathChildrenCache getInstance(CuratorFramework zk, String path) {
        return new ObservablePathChildrenCache(new OnSubscribeToPathChildren(zk, path));
    }

    private static class OnSubscribeToPathChildren implements OnSubscribe<ChildData> {
        CuratorFramework zk;
        String path;
        PathChildrenCache cache;
        //Map<String, Subject<ChildData, ChildData>> childStreams = new HashMap<>();
        PublishSubject<Observable<ChildData>> stream = PublishSubject.create();
        Map<String, ChildData> childs = new HashMap<>();
        Set<Subscriber<? super ChildData>> subscriberSet = new HashSet<>();

        public OnSubscribeToPathChildren(CuratorFramework zk, String path) {
            this.zk = zk;
            this.path = path;
            cache = new PathChildrenCache(zk, path, false/* Not cache data.*/);
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                    LOG.info("received child event {} for path {}", event, path);
                    switch (event.getType()) {
                        case CHILD_ADDED:
                            newChild(event.getData());
                            break;
                        case CHILD_UPDATED:
                            break;
                        case CHILD_REMOVED:
                            lostChild(event);
                            break;
                        case CONNECTION_SUSPENDED:
                            break;
                        case CONNECTION_RECONNECTED:
                            break;
                        case CONNECTION_LOST:
                            break;
                        case INITIALIZED:
                            break;
                    }
                }
            });

            try {
                cache.start();
            } catch (Exception e) {
                LOG.error("Cache start failed, reason {}", e.getCause());
            }
        }

        private void newChild(ChildData childData) {
            /*
            Subject<ChildData, ChildData> newStream = null;

            if (!childStreams.containsKey(childData.getPath())) {
                newStream = BehaviorSubject.create(childData);
                childStreams.put(childData.getPath(), newStream);
            }

            if (newStream != null) {
                stream.onNext(newStream);
            }
            */
            if (!childs.containsKey(childData.getPath())) {
                childs.put(childData.getPath(), childData);
                for (Subscriber<? super ChildData> subscriber : subscriberSet) {
                    subscriber.onNext(childData);
                }
            }
        }

        private void lostChild(PathChildrenCacheEvent event) {
            //Subject<ChildData, ChildData> subject = childStreams.remove(event.getData().getPath());
            if (childs.containsKey(event.getData().getPath())) {
                childs.remove(event.getData().getPath());
                for (Subscriber<? super ChildData> subscriber : subscriberSet) {
                    subscriber.onNext(null);
                }
            }
        }

        @Override
        public void call(Subscriber<? super ChildData> subscriber) {
            // Notify the initial datas...
            for (Map.Entry<String, ChildData> entry : childs.entrySet()) {
                subscriber.onNext(entry.getValue());
            }
            subscriberSet.add(subscriber);
        }
    }
}
