package no.nav.tms.utkast.config

import com.zaxxer.hikari.HikariDataSource

import kotliquery.Session
import kotliquery.sessionOf

interface Database {
    val dataSource: HikariDataSource
}
