/**
 * Formate un nombre de secondes en format HH:MM:SS
 * @param totalSeconds - Le nombre total de secondes
 * @returns Une chaîne formatée au format "H:MM:SS"
 */
export function formatTime(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;
  return `${hours}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
}
