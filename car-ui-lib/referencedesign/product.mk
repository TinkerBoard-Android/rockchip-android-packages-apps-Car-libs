# Inherit from this product to include the "Reference Design" RROs for CarUi

# Include shared library, commented out until fully implemented
#PRODUCT_PACKAGES += \
#    car-ui-lib-sharedlibrary \

PRODUCT_PRODUCT_PROPERTIES += ro.build.automotive.car.ui.shared.library.package.name=com.google.car.ui.sharedlibrary

PRODUCT_COPY_FILES += \
    packages/apps/Car/libs/car-ui-lib/referencedesign/car-ui-lib-preinstalled-packages.xml:system/etc/sysconfig/car-ui-lib-preinstalled-packages.xml \


# Include generated RROs
PRODUCT_PACKAGES += \
    googlecarui-com-google-android-apps-automotive-inputmethod \
    googlecarui-com-google-android-apps-automotive-inputmethod-dev \
    googlecarui-com-google-android-apps-automotive-templates-host \
    googlecarui-com-google-android-embedded-projection \
    googlecarui-com-google-android-gms \
    googlecarui-com-google-android-gsf \
    googlecarui-com-google-android-packageinstaller \
    googlecarui-com-google-android-carassistant \
    googlecarui-com-google-android-tts \
    googlecarui-com-android-vending \


# Include generated RROs that that use targetName
PRODUCT_PACKAGES += \
    googlecarui-overlayable-com-android-car-ui-paintbooth \
    googlecarui-overlayable-com-google-android-car-ui-paintbooth \
    googlecarui-overlayable-com-google-android-carui-ats \
    googlecarui-overlayable-com-android-car-rotaryplayground \
    googlecarui-overlayable-com-android-car-themeplayground \
    googlecarui-overlayable-com-android-car-carlauncher \
    googlecarui-overlayable-com-android-car-home \
    googlecarui-overlayable-com-android-car-media \
    googlecarui-overlayable-com-android-car-radio \
    googlecarui-overlayable-com-android-car-calendar \
    googlecarui-overlayable-com-android-car-companiondevicesupport \
    googlecarui-overlayable-com-android-car-systemupdater \
    googlecarui-overlayable-com-android-car-dialer \
    googlecarui-overlayable-com-android-car-linkviewer \
    googlecarui-overlayable-com-android-car-settings \
    googlecarui-overlayable-com-android-car-voicecontrol \
    googlecarui-overlayable-com-android-car-faceenroll \
    googlecarui-overlayable-com-android-managedprovisioning \
    googlecarui-overlayable-com-android-settings-intelligence \
    googlecarui-overlayable-com-google-android-apps-automotive-inputmethod \
    googlecarui-overlayable-com-google-android-apps-automotive-inputmethod-dev \
    googlecarui-overlayable-com-google-android-apps-automotive-templates-host \
    googlecarui-overlayable-com-google-android-embedded-projection \
    googlecarui-overlayable-com-google-android-gms \
    googlecarui-overlayable-com-google-android-gsf \
    googlecarui-overlayable-com-google-android-packageinstaller \
    googlecarui-overlayable-com-google-android-permissioncontroller \
    googlecarui-overlayable-com-google-android-carassistant \
    googlecarui-overlayable-com-google-android-tts \
    googlecarui-overlayable-com-android-vending \

# This system property is used to enable the RROs on startup via
# the requiredSystemPropertyName/Value attributes in the manifest
PRODUCT_PRODUCT_PROPERTIES += ro.build.car_ui_rros_enabled=true
