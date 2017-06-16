# business-registration

[![Build Status](https://travis-ci.org/hmrc/business-registration.svg)](https://travis-ci.org/hmrc/business-registration) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-registration/images/download.svg) ](https://bintray.com/hmrc/releases/business-registration/_latestVersion)

Prepop
-
<b>Contact details GET and POST URL:</b> /business-registration/:RegID/contact-details

<b>Contact details example json:</b>
{
  "firstName": "name1",
  "middleName": "m", //Optional
  "surname": "sName",
  "email": "email1", //Optional
  "telephoneNumber": "1", //Optional
  "mobileNumber": "2" //Optional
}

<b>Addresses GET and POST URL:</b>
/business-registration/:RegID/addresses

<b> Addresses example json: </b>
{
   "addressLine1": "add1",
    "addressLine2": "add2", //Optional
    "addressLine3": "add3", //Optional 
    "addressLine4": "add4", //Optional
    "postcode": "posty1", //Optional if country supplied
    "country": "country1" //Optional if postcode supplied
}

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")