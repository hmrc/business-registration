package config.filters

import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import uk.gov.hmrc.play.filters.{NoCacheFilter, RecoveryFilter}

/**
  * Created by jackie on 15/02/17.
  */
class MicroserviceFilters @Inject()(
                                     auditFilter: MicroserviceAuditFilter,
                                     loggingFilter: MicroserviceLoggingFilter,
                                     authFilter: MicroserviceAuthFilter
                                   ) extends DefaultHttpFilters(
  auditFilter,
  loggingFilter,
  authFilter,
  NoCacheFilter,
  RecoveryFilter
)



