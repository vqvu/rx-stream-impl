package local.vqvu.rxstream;

import static local.vqvu.rxstream.matcher.Emits.emits;
import static local.vqvu.rxstream.matcher.Emits.emitsValues;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import local.vqvu.rxstream.util.StreamItem;
import local.vqvu.util.Iterables;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class PublisherCreationTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void createErrorWorks() {
        RuntimeException e = new RuntimeException();
        SyncPublisher<Object> pub = Publishers.error(e);
        assertThat(pub, emits(StreamItem.error(e)));
        assertThat(pub, emits(StreamItem.error(e)));

        thrown.expect(RuntimeException.class);
        pub.iterator().hasNext();
    }

    @Test
    public void iterableWorks() {
        List<Integer> expected = Arrays.asList(1, 3, 5, 7);
        SyncPublisher<Integer> pub = Publishers.from(expected);
        assertThat(pub, emitsValues(expected));
        assertThat(pub, emitsValues(expected));
        assertThat(Iterables.asList(pub), equalTo(expected));
    }

    @Test
    public void iteratorWorks() {
        List<Integer> expected = Arrays.asList(1, 3, 5, 7);
        SyncPublisher<Integer> pub = Publishers.from(expected.iterator());
        assertThat(pub, emitsValues(expected));
        assertThat(pub, emits(StreamItem.<Integer>error(null)));
    }

    @Test
    public void createJustWorks() {
        List<Integer> expected = Arrays.asList(1, 3, 5, 7);
        SyncPublisher<Integer> pub = Publishers.just(1, 3, 5, 7);
        assertThat(pub, emitsValues(expected));
        assertThat(pub, emitsValues(expected));
        assertThat(Iterables.asList(pub), equalTo(expected));
    }

    @Test
    public void createConcatWorks() {
        List<Integer> expected = Arrays.asList(1, 3, 5, 7, 1, 3, 5, 7);
        Publisher<Integer> pub1 = Publishers.just(1, 3, 5, 7);
        Publisher<Integer> pub = pub1.concat(pub1);
        assertThat(pub, emitsValues(expected));
        assertThat(pub, emitsValues(expected));
        assertThat(Iterables.asList(pub.toSynchronousPublisher()), equalTo(expected));
    }

    @Test
    public void createBufferWorks() {
        List<List<Integer>> expected = Arrays.asList(Arrays.asList(1, 3),
                                                     Arrays.asList(5, 7),
                                                     Arrays.asList(9));
        Publisher<List<Integer>> pub = Publishers.just(1, 3, 5, 7, 9).buffer(2);
        assertThat(pub, emitsValues(expected));
        assertThat(pub, emitsValues(expected));
        assertThat(Iterables.asList(pub.toSynchronousPublisher()), equalTo(expected));
    }

    @Test
    public void badSubscriberShouldThrow() {
        Publishers.empty()
        .subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription sub) {
                sub.request(0);
            }

            @Override
            public void onNext(Object val) {
                fail("Should not call onNext.");
            }

            @Override
            public void onError(Throwable t) {
                String expectedMsg = "Argument to request() on an active "
                    + "Subscription must be positive. Actual: 0";
                assertThat(t, instanceOf(IllegalArgumentException.class));
                assertThat(t.getMessage(), equalTo(expectedMsg));
            }

            @Override
            public void onComplete() {
                fail("Should not call onComplete.");
            }
        });
    }

    @Test
    public void thereShouldBeNoRecursion() {
        Publishers.<Integer>createSync(() -> {
            return (cb) -> { cb.accept(StreamItem.value(0)); };
        }).subscribe(new Subscriber<Integer>() {
            Subscription sub;
            int count;

            @Override
            public void onSubscribe(Subscription sub) {
                this.sub = sub;
                this.sub.request(1);
            }

            @Override
            public void onNext(Integer t) {
                if (count++ < 100000) {
                    sub.request(1);
                }
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }
}
