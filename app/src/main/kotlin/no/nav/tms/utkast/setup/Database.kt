package no.nav.tms.utkast.setup

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Query
import kotliquery.action.ListResultQueryAction
import kotliquery.action.NullableResultQueryAction

import kotliquery.sessionOf
import kotliquery.using

interface Database {
    val dataSource: HikariDataSource
    fun update(queryBuilder: () -> Query) {
        using(sessionOf(dataSource)) {
            it.run(queryBuilder.invoke().asUpdate)
        }
    }

    fun <T> list(action: () -> ListResultQueryAction<T>): List<T> =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }

    fun <T> single(action: () -> NullableResultQueryAction<T>): T =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        } ?: throw IllegalStateException("Must return some value")
}
