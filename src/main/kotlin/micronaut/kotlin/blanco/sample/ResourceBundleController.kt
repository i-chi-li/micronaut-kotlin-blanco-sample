package micronaut.kotlin.blanco.sample

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import micronaut.kotlin.blanco.sample.blanco.resourcebundle.CommonResourceBundle
import org.slf4j.LoggerFactory

@Controller("/bundle")
class ResourceBundleController {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * curl -s http://localhost:8080/bundle?name=foo
     */
    @Get
    @Produces(MediaType.APPLICATION_JSON)
    fun index(name: String): String {
        return CommonResourceBundle().getI001(name)
    }
}
