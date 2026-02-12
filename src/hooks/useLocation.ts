import { useState, useRef, useEffect } from "react";
import { Animated } from "react-native";
import * as Location from "expo-location";

export interface Address {
  city: string;
  country: string;
}

export function useLocation() {
  const [address, setAddress] = useState<Address | null>(null);
  const [coordinates, setCoordinates] = useState<{ latitude: number; longitude: number } | null>(null);
  const [loadingLocation, setLoadingLocation] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);

  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(-20)).current;

  const requestLocation = async () => {
    setLoadingLocation(true);
    setPermissionDenied(false);

    try {
      const { status } = await Location.requestForegroundPermissionsAsync();

      if (status !== "granted") {
        setPermissionDenied(true);
        setLoadingLocation(false);
        return;
      }

      const location = await Location.getCurrentPositionAsync({});
      setCoordinates({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
      });

      const [reverseGeocode] = await Location.reverseGeocodeAsync({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
      });

      if (reverseGeocode) {
        setAddress({
          city: reverseGeocode.city ?? reverseGeocode.subregion ?? "Ville inconnue",
          country: reverseGeocode.country ?? "Pays inconnu",
        });
      }
    } catch (err) {
      setPermissionDenied(true);
    } finally {
      setLoadingLocation(false);
    }
  };

  const clearAddress = () => {
    setAddress(null);
    setPermissionDenied(false);
  };

  useEffect(() => {
    if (permissionDenied) {
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 300,
          useNativeDriver: true,
        }),
        Animated.spring(slideAnim, {
          toValue: 0,
          useNativeDriver: true,
        }),
      ]).start();
    } else {
      fadeAnim.setValue(0);
      slideAnim.setValue(-20);
    }
  }, [permissionDenied]);

  return {
    address,
    coordinates,
    loadingLocation,
    permissionDenied,
    fadeAnim,
    slideAnim,
    requestLocation,
    clearAddress,
  };
}
