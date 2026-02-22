import { formatTime } from "./timeHelper";

describe("formatTime", () => {
  it("formate 0 secondes en 0:00:00", () => {
    expect(formatTime(0)).toBe("0:00:00");
  });

  it("formate les secondes seules", () => {
    expect(formatTime(5)).toBe("0:00:05");
    expect(formatTime(59)).toBe("0:00:59");
  });

  it("formate les minutes et secondes", () => {
    expect(formatTime(60)).toBe("0:01:00");
    expect(formatTime(61)).toBe("0:01:01");
    expect(formatTime(125)).toBe("0:02:05");
  });

  it("formate les heures, minutes et secondes", () => {
    expect(formatTime(3600)).toBe("1:00:00");
    expect(formatTime(3661)).toBe("1:01:01");
    expect(formatTime(7322)).toBe("2:02:02");
  });

  it("padde les minutes et secondes à 2 chiffres", () => {
    expect(formatTime(3601)).toBe("1:00:01");
    expect(formatTime(3660)).toBe("1:01:00");
  });

  it("gère les grandes valeurs", () => {
    expect(formatTime(36000)).toBe("10:00:00");
  });
});
