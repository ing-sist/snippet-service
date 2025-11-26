//package ingsist.snippet.config
//import ingsist.snippet.domain.parser.ExternalParserAdapter
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import ingsist.snippet.domain.parser.ParserRegistry
//import ingsist.snippet.domain.parser.ValidationResult
//
//@Configuration
//class ExternalParsersRegister {
//
//    @Bean
//    fun registerExternalParsers(parserRegistry: ParserRegistry): Unit {
//        val external = // llamo al ps parser
//        val printsccriptValidate: (String) -> Any = { code ->
//            try {
//                external.validate(code)
//            } catch (ex: Exception) {
//
//            }
//        }
//
//        val printScriptMapper: (Any) -> ValidationResult = { ext ->
//            if(ext.isValid) ValidationResult.Valid
//            else ValidationResult.Invalid(ext.error)
//        }
//
//        val printScriptAdapter = ExternalParserAdapter(printsccriptValidate, printScriptMapper)
//        parserRegistry.registerParser("printScript", "1.0", printScriptAdapter)
//    }
//}