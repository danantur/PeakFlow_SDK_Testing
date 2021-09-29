package com.dantesting.peakflow_sdk_testing.utils

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.jetbrains.annotations.Nullable
import java.lang.Exception

interface Value<T> {
    var value: T
}

class ValueObservable<T> : Value<T> {

    override var value: T
        set(value) {
            subject.onNext(value!!)
            field = value
        }
    private val subject: BehaviorSubject<T>

    constructor(value: T) {
        if (value is Nullable)
            throw Exception("Type must be not Nullable")
        this.subject = BehaviorSubject.createDefault(value)
        this.value = value
    }

    fun subscribe(onNext: (value: T) -> Unit, onError: (throwable: Throwable) -> Unit): Disposable
        = subject.map { it }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe (onNext, onError)
}
