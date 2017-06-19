# business-registration

[![Build Status](https://travis-ci.org/hmrc/business-registration.svg)](https://travis-ci.org/hmrc/business-registration) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-registration/images/download.svg) ](https://bintray.com/hmrc/releases/business-registration/_latestVersion)

Prepop
------

| Path                                                             | Supported Methods | Description  |
| -----------------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/contact-details```      |        GET        | Retrieves the contact details for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 403           | Forbidden     |
| 404           | Not found     |

####Example of usage
```
GET /business-registration/12345/contact-details

Header:
    Authorization Bearer fNAao9C4kTby8cqa6g75emw1DZIyA5B72nr9oKHHetE=
```

A ```200``` success response:

```json
{
   "firstName": "fName",  //Optional
   "middleName": "mName", //Optional
   "surname": "sName",  //Optional
   "email": "email1", //Optional
   "telephoneNumber": "012345", //Optional
   "mobileNumber": "012345" //Optional
}
```

#

| Path                                                             | Supported Methods | Description  |
| -----------------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/contact-details```      |        POST       | Inserts / updates a set of contact details for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 403           | Forbidden     |
| 404           | Not found     |

**Request body**

```json
{
   "firstName": "fName",  //Optional
   "middleName": "mName", //Optional
   "surname": "sName",  //Optional
   "email": "email1", //Optional
   "telephoneNumber": "012345", //Optional
   "mobileNumber": "012345" //Optional
}
```


####Example of usage
```
POST /business-registration/12345/contact-details

Header:
    Authorization Bearer fNAao9C4kTby8cqa6g75emw1DZIyA5B72nr9oKHHetE=
    
Request body:
    {
       "firstName": "John",
       "middleName": "Martin",
       "surname": "Smith",
       "email": "example@email.co.uk",
       "telephoneNumber": "01234567890",
       "mobileNumber": "01234567890"
    }
```

A ```200``` success response: **No Response body**

#

| Path                                                       | Supported Methods | Description  |
| -----------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/addresses```      |        GET        | Fetches all addresses for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 403           | Forbidden     |
| 404           | Not found     |

####Example of usage
```
GET /business-registration/12345/addresses

Header:
    Authorization Bearer fNAao9C4kTby8cqa6g75emw1DZIyA5B72nr9oKHHetE=
```

A ```200``` success response:

```json
{
    "addresses": {
        [
           {
               "addressLine1": "add1",
               "addressLine2": "add2", //Optional
               "addressLine3": "add3", //Optional 
               "addressLine4": "add4", //Optional
               "postcode": "posty1", //Optional if country supplied
               "country": "country1" //Optional if postcode supplied
           },
           {
               "addressLine1": "add1",
               "addressLine2": "add2", //Optional
               "addressLine3": "add3", //Optional 
               "addressLine4": "add4", //Optional
               "postcode": "posty1", //Optional if country supplied
               "country": "country1" //Optional if postcode supplied
          }
        ]
    }
}
```

#

| Path                                                       | Supported Methods | Description  |
| -----------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/addresses```      |        POST       | Insert / Update a single address for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 403           | Forbidden     |

####Example of usage
```
POST /business-registration/12345/addresses

Header:
    Authorization Bearer fNAao9C4kTby8cqa6g75emw1DZIyA5B72nr9oKHHetE=

Request Body:
    {
        "addressLine1" : "14 St Philips place",
        "addressLine2": "Moreley",
        "addressLine3": "Telford",
        "addressLine4": "Shropshire",
        "postcode" : "TF11FG",
        "country" : "UK"
    }
```

A ```200``` success response: **No Response body**

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")