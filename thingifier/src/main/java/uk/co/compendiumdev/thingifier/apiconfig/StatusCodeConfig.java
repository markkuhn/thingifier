package uk.co.compendiumdev.thingifier.apiconfig;

/* configurable because different apis return different codes under different situations
    e.g. some APIs are 400 for every client error

    //todo: have some high level methods e.g. set400ForAllClientErrors()
 */
public class StatusCodeConfig {

    // making setting public at the moment

    /* client side errors */

    public int acceptTypeNotSupportedValue; // 406

    public StatusCodeConfig(){
        resetClientSideErrorStatusCodes();
    }

    public void resetClientSideErrorStatusCodes(){
        acceptTypeNotSupportedValue=406;
    }

    public int acceptTypeNotSupported() {
        return acceptTypeNotSupportedValue;
    }

}
