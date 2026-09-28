package com.callankan.poloapp.di

import com.callankan.poloapp.data.backup.BackupManager
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.ComplianceRepository
import com.callankan.poloapp.data.repository.FinanceRepository
import com.callankan.poloapp.data.repository.FuelRepository
import com.callankan.poloapp.data.repository.LogbookRepository
import com.callankan.poloapp.data.repository.MaintenanceRepository
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.report.ReportGenerator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Acceso a los repositorios fuera de las clases inyectadas (tests de extremo a extremo). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun active(): ActiveVehicle
    fun vehicles(): VehicleRepository
    fun fuel(): FuelRepository
    fun maintenance(): MaintenanceRepository
    fun finance(): FinanceRepository
    fun compliance(): ComplianceRepository
    fun logbook(): LogbookRepository
    fun report(): ReportGenerator
    fun backup(): BackupManager
}
