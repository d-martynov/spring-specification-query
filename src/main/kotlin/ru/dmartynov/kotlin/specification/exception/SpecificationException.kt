package ru.dmartynov.kotlin.specification.exception

class SpecificationException : RuntimeException {
    constructor() {}

    constructor(string: String) : super(string) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    constructor(cause: Throwable) : super(cause) {}

}
