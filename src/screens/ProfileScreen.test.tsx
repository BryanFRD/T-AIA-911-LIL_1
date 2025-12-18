import { render, screen } from '@testing-library/react-native';
import ProfileScreen from '@/screens/ProfileScreen';

describe('ProfileScreen', () => {
  it('should render profile title', () => {
    render(<ProfileScreen />);
    expect(screen.getByText('Profile')).toBeTruthy();
  });

  it('should render profile information text', () => {
    render(<ProfileScreen />);
    expect(screen.getByText('Your profile information here')).toBeTruthy();
  });
});
