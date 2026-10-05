# GoldTimeCo
# INSY7315 – Task 2 | Emeris 2026

# Members

ST10444715	Keegan Ewan Tromp
ST10434249	Richard Hein
ST10443463	Yashin Bhawanideen
ST10443090	Amir Muller
ST10450208	Mishal Bhikha

# File paths
Json files are separately commited, if you want to run the application you will need to do this first:
- firebase-service-account key location in the file: C:\GoldTime\GoldTimeCo\GoldTimeApi
- google-service key location in the file: C:\GoldTime\GoldTimeCo\GoldTimeApp\app


# Where the Application Is Hosted
API: The ASP.NET Web API is accessible from any device, not just a local computer, because it is hosted on Azure App Service.
Product photos are kept in Azure. Both the images and the API remain lightweight because the API delivers the image links rather than serving the files.
User data and authentication are handled by Firebase (password and email authentication) and kept in Firebase Firestore.
Payments: Managed by PayFast, an API-integrated sandbox environment.

# YouTube Links
The YouTube links explaining how the code and application work are included in the PowerPoint presentation. They are also listed here for convenience:

YouTube Links: 
Login, Register and Home Page: https://youtu.be/hJtkLsMFPWs 
Product page, product details and quote form: https://youtu.be/cJvfos4OMO4 
Cart page: https://youtu.be/7BuSTCY9ebQ 
Checkout Procedure: https://youtu.be/xdkp0lNnlsU 
Profile Page & order history: https://youtu.be/a3DNP-A6znU 

# Key Features and Functionality
What Is the App?
For Gold Time Co., a business that purchases and sells watches, gold and silver bars, and bullion like Krugerrands and collectibles, GoldTimeCo is an Android mobile trading platform. Instead of calling the company directly, customers use mobile phones to register, peruse the complete product catalogue, and make purchases or sales. A dashboard is provided to managers so they may examine and manage the ensuing orders, payments, and bids.

Front End
The client program is a native Android application made with Kotlin in Android Studio. Users can view the company's assets on the main page after registering or logging in. A navigation bar connects main, Shop, Sell Gold, and Orders. Each item's weight, purity, condition, and stock availability are shown in the product browser. Users can add items to a cart, adjust quantities, see a running total, and get a quote prior to checking out. A delivery form, an order review, payment, and a confirmation page are all part of the checkout process.

APIs and Back End
On the back end, the application communicates with an ASP.NET Web API. It manages orders, quotation requests, and sale requests in addition to providing product data. The manager dashboard receives new orders and quotes. The API integrates PayFast, a South African payment gateway that takes card and rapid EFT payments. We utilised the PayFast sandbox environment to securely demonstrate the complete payment procedure. Before sending users back to a success page, the software directs them to PayFast for payment.

Data and Hosting
User accounts are managed by Firebase email and password authentication, and user data is stored in Firebase Firestore. The API is accessible from any device since it is hosted on Azure App Service. Product images are stored on Azure, and the API provides URLs to them.

# How the System Meets the Requirements and Non-Functional Expectations
Task 1's basic functional requirements are met by the system. Consumers can use PayFast to safely pay after registering, logging in, browsing products, filtering categories, adding items to a basket, and requesting estimates. Without having to speak with customers, managers may review incoming orders and bids.

- Security: Firebase Authentication handles user credentials, eliminating the need to keep raw passwords, and PayFast's tokenisation and encryption of card data enhance security.
- Performance and scalability are supported by cloud hosting on Firebase and Azure.
reliability: Managed cloud services offer reliability.
- Usability is given priority in a simple customer flow (browse, quotation, add to cart, check out).
- Maintainability: The Android client and Web API are kept apart, allowing for independent changes in accordance with the service-oriented architecture outlined in the design documentation.

# Technical Decisions, Design Choices and Challenges Overcome
Technical Choices
For a modern, native Android experience, use Kotlin with Android Studio.
ASP.NET Web API: For a well-organised, secure back end that complemented the team's knowledge of.NET.
Firebase Firestore: Because of its flexible document format, which works well with orders, sell requests, and products.
Firebase Authentication: To handle accounts securely.
PayFast: We chose not to create our own payment system since it is plug-and-play, controls compliance, and is tailored for the South African market.

# Design Choices
Users are prevented from getting lost with a simple checkout procedure and an easy-to-use navigation bar.
Before obtaining a quote or making a purchase, buyers may make educated judgements by viewing product information up front.

# Challenges Overcome
changed the API hosting from Firebase to Azure App Service, which is a more suitable and reliable configuration for an ASP.NET API.
To enable the API to transmit links instead of files, product images were moved from the local directories of the API onto Azure storage.
Instead of only using an emulator, the app was set up and tested on a real phone, which showed connection issues that needed to be resolved.
incorporated PayFast's sandbox into the API to ensure a seamless payment procedure.
Resolved Gradle sync problems brought on by the Android Studio project structure and project folder layout.

