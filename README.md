# business-registration

Part of the SCRS (Streamlined Company Registration Service) suite which is a joint service between HMRC and Companies House allowing a user to Incorporate a Limited Company, register for Corporation Tax and optionally register for PAYE
 
It can be started via Service Manager with the command sm --start SCRS_ALL -r

Alternatively to start individually use the run.sh command which will start the service on port 9660

Prepop.
------
| Path                                                             | Supported Methods | Description  |
| -----------------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/trading-name```      |        GET        | Retrieves the trading name for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 204           | No Content    |
| 403           | Forbidden     |

**Example of usage**
```
GET /business-registration/12345/trading-name

Header:
    Authorization Bearer fNAao9C4kTby8cqa6g75emw1DZIyA5B72nr9oKHHetE=
```

A ```200``` success response:

```json
{
   "tradingName":"yourTradingName" 
}
```

#

| Path                                                             | Supported Methods | Description  |
| -----------------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/trading-name```      |        POST       | Inserts / updates a trading name for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 403           | Forbidden     |
| 500           | Internal Server Error    |

**Request body**

```json
{
   "tradingName": "yourTradingName"
}
```


**Example of usage**
```
POST /business-registration/12345/trading-name

Header:
    Authorization Bearer fNAao9C4kTby8cqa6g75emw1DZIyA5B72nr9oKHHetE=
    
Request body:
    {
       "tradingName": "yourTradingName"
    }
```

A ```200``` success response:
```json
{
    "tradingName": "yourTradingName"
}
```

#

| Path                                                             | Supported Methods | Description  |
| -----------------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/contact-details```      |        GET        | Retrieves the contact details for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 403           | Forbidden     |
| 404           | Not found     |

**Example of usage**
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


**Example of usage**
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

> **Important**
> When inserting / updating addresses we only validate that an addressLine1 AND (postcode OR country) exist to check equality between addresses,
> therefore, any other address related data (e.g. address audit refs) your service requires can be freely inserted / updated / retrieved

| Path                                                       | Supported Methods | Description  |
| -----------------------------------------------------------| ------------------| ------------ |
|```/business-registration/:RegistrationID/addresses```      |        GET        | Fetches all addresses for a given users' registrationID |

    Responds with:
    
| Status        | Message       |
|:--------------|:--------------|
| 200           | OK            |
| 403           | Forbidden     |
| 404           | Not found     |

**Example of usage**
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

**Example of usage**
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