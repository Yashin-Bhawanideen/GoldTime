# Gold Time Co

# Basic Information

All memebers have access to both the Azure API hosted services Firebase database and blob storage (Azure for images), I have added you guys using the emails you have sent to the group:

- trompkeegan@gmail.com
- Mbhikha@outlook.com
- Heinrc0@gmail.com
- Amir.muller@gmail.com

The API is Cloud hosted on Azure
The Android App is hosted on Firebase

# How to run the program

Open Android Studio and choose the GoldTimeCo folder, in the folder choose the GoldTimeApp (android) folder, Android studio will sync the project automatically. 

-> A more comprehensive path: Android studio/Open
                                            /GoldTimeCo
                                             /GoldTimeApp


The API is hosted already therefore, you don't need to run the API separately. You can run the app straight from the Android Studio platform onto your emulator or physical device.

# If there are any changes in the API code

If you have added any changes to the code for the API, You have to re-publish the API to Azure again, Here are some image to help you. 2 Simple ways:

1. Click Publish and the API will re-publish

<img width="1692" height="800" alt="image" src="https://github.com/user-attachments/assets/4af22a3a-1203-4216-b721-1f612ff7b60d" />

2. This tab  in your browser will open, and that's how you know its working

<img width="1696" height="690" alt="image" src="https://github.com/user-attachments/assets/e682c474-4b1d-4aa8-875d-6b47750b99b1" />

# How to add images

Watch this video on how to add the images to Azure then using the provided url in the code: 

https://youtu.be/y8FURnYPEFA

* Images can be jpeg, jpg or png
* Ensure that the name of you images don't have spaces: 
eg. Silver-bar (correct)
    Sliver bar (not correct)

# JSON files

Place both the files where its supposed to be, same like we did in Prog7314:

* google-services.json location path: GoldTimeCo/GoldTimeApp/app (download the Android configuration for the correct Firebase project).

* For local API credentials, keep the service-account file outside the repository and set GOOGLE_APPLICATION_CREDENTIALS to its absolute path. On Azure, the existing API reads Firebase__CredentialsJson and Firebase__ProjectId from application settings. Do not commit service-account keys or put them in Android. The previously tracked JSON archive must not be treated as a safe credential source; its exposed key needs rotation by the account owner.

## Sector 4 checkout status

Delivery Details, Review Order and Payment Method Selection currently work together in the Compose sample preview. They are not yet connected to the real Cart, app navigation or PayFast. A preview confirmation does not mean an order was placed or paid.

The pending-order API has been added and locally built/tested. It requires authenticated requests, database product prices/stock and a configured delivery fee. Deployment and live database writes remain unverified. See [checkout API setup and tests](docs/checkout-api.md) and [Sector 4 progress and rubric evidence](docs/sector4-progress.md) before integration. Payment notification verification, stock reservation and payment-success confirmation remain to be implemented.

# Videos

Once you are done with your part, please take create a video of you explaining what you did for your part and how the code works. Send the mp4 video file to (Yashin - 079 375 3759), for the PowerPoint Presentation. Yashin will convert the video to a Youtube link.


