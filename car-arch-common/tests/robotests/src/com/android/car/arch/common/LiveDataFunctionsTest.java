/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.arch.common;

import static com.android.car.arch.common.LiveDataFunctions.dataOf;
import static com.android.car.arch.common.LiveDataFunctions.emitsNull;
import static com.android.car.arch.common.LiveDataFunctions.falseLiveData;
import static com.android.car.arch.common.LiveDataFunctions.ifThenElse;
import static com.android.car.arch.common.LiveDataFunctions.not;
import static com.android.car.arch.common.LiveDataFunctions.nullLiveData;
import static com.android.car.arch.common.LiveDataFunctions.split;
import static com.android.car.arch.common.LiveDataFunctions.trueLiveData;

import static com.google.common.truth.Truth.assertThat;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.arch.common.testing.CaptureObserver;
import com.android.car.arch.common.testing.TestLifecycleOwner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LiveDataFunctionsTest {

    @Rule
    public final InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();
    @Rule
    public final TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();

    @Test
    public void testNullLiveData() {
        CaptureObserver<Object> observer = new CaptureObserver<>();
        nullLiveData().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
        assertThat(nullLiveData().getValue()).isNull();
    }

    @Test
    public void testTrueLiveData() {
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        trueLiveData().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isTrue();
        assertThat(trueLiveData().getValue()).isTrue();
    }

    @Test
    public void testFalseLiveData() {
        CaptureObserver<Boolean> observer = new CaptureObserver<>();
        falseLiveData().observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isFalse();
        assertThat(falseLiveData().getValue()).isFalse();
    }

    @Test
    public void testNot() {
        testUnaryOperator(
                LiveDataFunctions::not,
                pair(trueLiveData(), false),
                pair(falseLiveData(), true),
                pair(nullLiveData(), null));

        checkUninitialized(not(new MutableLiveData<>()));
    }

    @Test
    public void testEmitsNull() {
        testUnaryOperator(
                LiveDataFunctions::emitsNull,
                pair(dataOf(new Object()), false),
                pair(nullLiveData(), true));
        checkUninitialized(emitsNull(new MutableLiveData<>()));
    }

    @Test
    public void testAnd_truthTable() {
        testBinaryOperator(
                LiveDataFunctions::and,
                pair(pair(trueLiveData(), trueLiveData()), true),
                pair(pair(trueLiveData(), falseLiveData()), false),
                pair(pair(trueLiveData(), nullLiveData()), null),
                pair(pair(falseLiveData(), trueLiveData()), false),
                pair(pair(falseLiveData(), falseLiveData()), false),
                pair(pair(falseLiveData(), nullLiveData()), false),
                pair(pair(nullLiveData(), trueLiveData()), null),
                pair(pair(nullLiveData(), falseLiveData()), false),
                pair(pair(nullLiveData(), nullLiveData()), null));
    }

    @Test
    public void testAnd_uninitialized() {
        MutableLiveData<Boolean> empty = new MutableLiveData<>();
        checkUninitializedBinary(
                LiveDataFunctions::and,
                pair(trueLiveData(), empty),
                pair(falseLiveData(), empty),
                pair(nullLiveData(), empty),
                pair(empty, trueLiveData()),
                pair(empty, falseLiveData()),
                pair(empty, nullLiveData()));
    }

    @Test
    public void testAnd_changeValue() {
        MutableLiveData<Boolean> source = new MutableLiveData<>();
        CaptureObserver<Boolean> observer = new CaptureObserver<>();

        LiveDataFunctions.and(trueLiveData(), source).observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isFalse();

        source.setValue(true);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isTrue();
    }

    @Test
    public void testOr_truthTable() {
        testBinaryOperator(
                LiveDataFunctions::or,
                pair(pair(trueLiveData(), trueLiveData()), true),
                pair(pair(trueLiveData(), falseLiveData()), true),
                pair(pair(trueLiveData(), nullLiveData()), true),
                pair(pair(falseLiveData(), trueLiveData()), true),
                pair(pair(falseLiveData(), falseLiveData()), false),
                pair(pair(falseLiveData(), nullLiveData()), null),
                pair(pair(nullLiveData(), trueLiveData()), true),
                pair(pair(nullLiveData(), falseLiveData()), null),
                pair(pair(nullLiveData(), nullLiveData()), null));
    }

    @Test
    public void testOr_uninitialized() {
        LiveData<Boolean> empty = new MutableLiveData<>();
        checkUninitializedBinary(
                LiveDataFunctions::or,
                pair(trueLiveData(), empty),
                pair(falseLiveData(), empty),
                pair(nullLiveData(), empty),
                pair(empty, trueLiveData()),
                pair(empty, falseLiveData()),
                pair(empty, nullLiveData()));
    }

    @Test
    public void testOr_changeValue() {
        MutableLiveData<Boolean> source = new MutableLiveData<>();
        CaptureObserver<Boolean> observer = new CaptureObserver<>();

        LiveDataFunctions.or(trueLiveData(), source).observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isFalse();

        source.setValue(true);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isTrue();
    }

    @Test
    public void testIff_truthTable() {
        Object object = new Object();
        testBinaryOperator(
                (predicate, value) -> LiveDataFunctions.iff(predicate, Boolean::booleanValue,
                        value),
                pair(pair(trueLiveData(), dataOf(object)), object),
                pair(pair(falseLiveData(), dataOf(object)), null),
                pair(pair(nullLiveData(), dataOf(object)), null));
    }

    @Test
    public void testIff_uninitialized() {
        checkUninitializedBinary(
                (predicate, value) -> LiveDataFunctions.iff(predicate, Boolean::booleanValue,
                        value),
                pair(new MutableLiveData<>(), dataOf(new Object())),
                pair(falseLiveData(), new MutableLiveData<>()),
                pair(trueLiveData(), new MutableLiveData<>()));
    }

    @Test
    public void testIff_changePredicate() {
        MutableLiveData<Boolean> predicate = new MutableLiveData<>();
        MutableLiveData<Object> value = new MutableLiveData<>();
        Object valueObject = new Object();
        value.setValue(valueObject);
        CaptureObserver<Object> observer = new CaptureObserver<>();

        LiveDataFunctions.iff(predicate, Boolean::booleanValue, value)
                .observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isFalse();

        predicate.setValue(false);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
        observer.reset();

        predicate.setValue(true);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(valueObject);
        observer.reset();

        predicate.setValue(null);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
    }

    @Test
    public void testIff_changeValue() {
        LiveData<Boolean> predicate = trueLiveData();
        MutableLiveData<Object> value = new MutableLiveData<>();
        Object firstObject = new Object();
        CaptureObserver<Object> observer = new CaptureObserver<>();

        LiveDataFunctions.iff(predicate, Boolean::booleanValue, value)
                .observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isFalse();

        value.setValue(null);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
        observer.reset();

        value.setValue(firstObject);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(firstObject);
        observer.reset();

        value.setValue(new Object());

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNotSameAs(firstObject);
    }

    @Test
    public void testIff_changeValue_doesntNotifyForIrrelevantChanges() {
        MutableLiveData<Object> irrelevantValue = new MutableLiveData<>();
        irrelevantValue.setValue(new Object());
        CaptureObserver<Object> observer = new CaptureObserver<>();

        // irrelevantValue irrelevant because iff() always emits null when predicate is false.
        LiveDataFunctions.iff(falseLiveData(), Boolean::booleanValue, irrelevantValue)
                .observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isTrue();
        observer.reset();

        irrelevantValue.setValue(null);

        assertThat(observer.hasBeenNotified()).isFalse();
    }

    @Test
    public void testIfThenElse_liveDataParams_truthTable() {
        Object trueObject = new Object();
        Object falseObject = new Object();

        LiveData<Object> trueObjectData = dataOf(trueObject);
        LiveData<Object> falseObjectData = dataOf(falseObject);

        testOperator(
                arg ->
                        () ->
                                ifThenElse(
                                        arg.mPredicate, Boolean::booleanValue, arg.mTrueData,
                                        arg.mFalseData),
                pair(
                        new IfThenElseDataParams<>(trueLiveData(), trueObjectData, falseObjectData),
                        trueObject),
                pair(
                        new IfThenElseDataParams<>(falseLiveData(), trueObjectData,
                                falseObjectData),
                        falseObject),
                pair(
                        new IfThenElseDataParams<>(dataOf(null), trueObjectData, falseObjectData),
                        null));
    }

    @Test
    public void testIfThenElse_liveDataParams_uninitialized() {
        Object trueObject = new Object();
        Object falseObject = new Object();

        LiveData<Object> trueObjectData = dataOf(trueObject);
        LiveData<Object> falseObjectData = dataOf(falseObject);

        checkUninitialized(
                ifThenElse(
                        new MutableLiveData<>(), Boolean::booleanValue, trueObjectData,
                        falseObjectData));
        checkUninitialized(
                ifThenElse(
                        trueLiveData(), Boolean::booleanValue, new MutableLiveData<>(),
                        falseObjectData));
        checkUninitialized(
                ifThenElse(
                        falseLiveData(), Boolean::booleanValue, trueObjectData,
                        new MutableLiveData<>()));
    }

    @Test
    public void testIfThenElse_liveDataParams_changePredicate() {
        Object trueObject = new Object();
        Object falseObject = new Object();

        LiveData<Object> trueObjectData = dataOf(trueObject);
        LiveData<Object> falseObjectData = dataOf(falseObject);

        MutableLiveData<Boolean> predicate = new MutableLiveData<>();
        CaptureObserver<Object> observer = new CaptureObserver<>();

        ifThenElse(predicate, Boolean::booleanValue, trueObjectData,
                falseObjectData)
                .observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isFalse();

        predicate.setValue(false);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(falseObject);
        observer.reset();

        predicate.setValue(true);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(trueObject);
        observer.reset();

        predicate.setValue(null);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
    }

    @Test
    public void testIfThenElse_liveDataParams_changeValue_value() {
        Object trueObject = new Object();
        Object falseObject = new Object();

        MutableLiveData<Object> trueObjectData = new MutableLiveData<>();
        LiveData<Object> falseObjectData = dataOf(falseObject);

        LiveData<Boolean> predicate = trueLiveData();
        CaptureObserver<Object> observer = new CaptureObserver<>();

        ifThenElse(predicate, Boolean::booleanValue, trueObjectData,
                falseObjectData)
                .observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isFalse();

        trueObjectData.setValue(null);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
        observer.reset();

        trueObjectData.setValue(trueObject);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(trueObject);
        observer.reset();

        trueObjectData.setValue(new Object());

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNotSameAs(trueObject);
    }

    @Test
    public void testIfThenElse_liveDataParams_changeValue_doesntNotifyForIrrelevantChanges() {
        MutableLiveData<Object> irrelevantValue = new MutableLiveData<>();
        irrelevantValue.setValue(new Object());
        CaptureObserver<Object> observer = new CaptureObserver<>();

        // irrelevantValue irrelevant because ifThenElse() is backed by other param when
        // predicate is
        // false.
        ifThenElse(
                falseLiveData(), Boolean::booleanValue, irrelevantValue, dataOf(new Object()))
                .observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isTrue();
        observer.reset();

        irrelevantValue.setValue(null);

        assertThat(observer.hasBeenNotified()).isFalse();
    }

    @Test
    public void testIfThenElse_valueParams_truthTable() {
        Object trueObject = new Object();
        Object falseObject = new Object();

        testOperator(arg -> () -> ifThenElse(arg.mPredicate, Boolean::booleanValue, arg.mTrueValue,
                arg.mFalseValue),
                pair(new IfThenElseValueParams<>(trueLiveData(), trueObject, falseObject),
                        trueObject),
                pair(new IfThenElseValueParams<>(falseLiveData(), trueObject, falseObject),
                        falseObject),
                pair(new IfThenElseValueParams<>(dataOf(null), trueObject, falseObject), null));
    }

    @Test
    public void testIfThenElse_valueParams_uninitialized() {
        Object trueObject = new Object();
        Object falseObject = new Object();

        checkUninitialized(
                ifThenElse(new MutableLiveData<>(), Boolean::booleanValue, trueObject,
                        falseObject));
    }

    @Test
    public void testIfThenElse_valueParams_changePredicate() {
        Object trueObject = new Object();
        Object falseObject = new Object();

        MutableLiveData<Boolean> predicate = new MutableLiveData<>();
        CaptureObserver<Object> observer = new CaptureObserver<>();

        ifThenElse(predicate, Boolean::booleanValue, trueObject, falseObject)
                .observe(mLifecycleOwner, observer);
        assertThat(observer.hasBeenNotified()).isFalse();

        predicate.setValue(false);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(falseObject);
        observer.reset();

        predicate.setValue(true);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isSameAs(trueObject);
        observer.reset();

        predicate.setValue(null);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isNull();
    }

    @Test
    public void testSplit() {
        Object first = new Object();
        Object second = new Object();

        MutableLiveData<Object> firstData = new MutableLiveData<>();
        MutableLiveData<Object> secondData = new MutableLiveData<>();
        firstData.setValue(first);
        secondData.setValue(second);

        Object[] observedValues = new Object[2];
        boolean[] notified = new boolean[1];

        LiveDataFunctions.pair(firstData, secondData)
                .observe(
                        mLifecycleOwner,
                        split(
                                (left, right) -> {
                                    notified[0] = true;
                                    observedValues[0] = left;
                                    observedValues[1] = right;
                                }));

        assertThat(notified[0]).isTrue();
        assertThat(observedValues[0]).isSameAs(first);
        assertThat(observedValues[1]).isSameAs(second);
    }

    @Test
    public void testSplit_null() {
        Object[] observedValues = new Object[2];
        boolean[] notified = new boolean[1];

        dataOf((Pair<Object, Object>) null)
                .observe(
                        mLifecycleOwner,
                        split(
                                (left, right) -> {
                                    notified[0] = true;
                                    observedValues[0] = left;
                                    observedValues[1] = right;
                                }));

        assertThat(notified[0]).isTrue();
        assertThat(observedValues[0]).isNull();
        assertThat(observedValues[1]).isNull();
    }

    @Test
    public void testCombine() {
        Object first = new Object();
        Object second = new Object();

        MutableLiveData<Object> firstData = new MutableLiveData<>();
        MutableLiveData<Object> secondData = new MutableLiveData<>();
        firstData.setValue(first);
        secondData.setValue(second);

        CaptureObserver<Pair<Object, Object>> observer = new CaptureObserver<>();
        LiveDataFunctions.combine(firstData, secondData, Pair::new).observe(mLifecycleOwner,
                observer);

        Pair<Object, Object> observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.first).isSameAs(first);
        assertThat(observedValue.second).isSameAs(second);

        Object third = new Object();
        firstData.setValue(third);

        observedValue = observer.getObservedValue();
        assertThat(observedValue).isNotNull();
        assertThat(observedValue.first).isSameAs(third);
        assertThat(observedValue.second).isSameAs(second);
    }

    private static class IfThenElseDataParams<T> {
        final LiveData<Boolean> mPredicate;
        final LiveData<T> mTrueData;
        final LiveData<T> mFalseData;

        private IfThenElseDataParams(
                LiveData<Boolean> predicate, LiveData<T> trueData, LiveData<T> falseData) {
            this.mPredicate = predicate;
            this.mTrueData = trueData;
            this.mFalseData = falseData;
        }
    }

    private static class IfThenElseValueParams<T> {
        final LiveData<Boolean> mPredicate;
        final T mTrueValue;
        final T mFalseValue;

        private IfThenElseValueParams(LiveData<Boolean> predicate, T trueValue, T falseValue) {
            this.mPredicate = predicate;
            this.mTrueValue = trueValue;
            this.mFalseValue = falseValue;
        }
    }

    private <R> void testOperator(Supplier<LiveData<R>> op, R result) {
        CaptureObserver<R> observer = new CaptureObserver<>();
        LiveData<R> data = op.get();

        data.observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isTrue();
        assertThat(observer.getObservedValue()).isEqualTo(result);
        assertThat(data.getValue()).isEqualTo(result);
    }

    @SafeVarargs // args are never written to
    private final <P, R> void testOperator(
            Function<P, Supplier<LiveData<R>>> ops, Pair<P, R>... args) {
        for (Pair<P, R> arg : args) {
            testOperator(ops.apply(arg.first), arg.second);
        }
    }

    @SafeVarargs // args are never written to
    private final <T, R> void testUnaryOperator(
            Function<LiveData<T>, LiveData<R>> op, Pair<LiveData<T>, R>... args) {
        testOperator(arg -> () -> op.apply(arg), args);
    }

    @SafeVarargs // args are never written to
    private final <A, B, R> void testBinaryOperator(
            BiFunction<LiveData<A>, LiveData<B>, LiveData<R>> op,
            Pair<Pair<LiveData<A>, LiveData<B>>, R>... args) {
        testOperator(arg -> () -> op.apply(arg.first, arg.second), args);
    }

    private <T, R> Pair<T, R> pair(T first, R second) {
        return new Pair<>(first, second);
    }

    private <T> void checkUninitialized(LiveData<T> liveData) {
        CaptureObserver<T> observer = new CaptureObserver<>();

        liveData.observe(mLifecycleOwner, observer);

        assertThat(observer.hasBeenNotified()).isFalse();
        assertThat(liveData.getValue()).isNull();
    }

    @SafeVarargs // args are never written to
    private final <A, B> void checkUninitializedBinary(
            BiFunction<LiveData<A>, LiveData<B>, LiveData<?>> op,
            Pair<LiveData<A>, LiveData<B>>... args) {
        for (Pair<LiveData<A>, LiveData<B>> arg : args) {
            checkUninitialized(op.apply(arg.first, arg.second));
        }
    }
}
