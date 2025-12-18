import { render, screen, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import HomeScreen from '@/screens/HomeScreen';

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={createTestQueryClient()}>
    {children}
  </QueryClientProvider>
);

describe('HomeScreen', () => {
  it('should show loading state initially', () => {
    render(<HomeScreen />, { wrapper });
    expect(screen.getByText('Loading users...')).toBeTruthy();
  });

  it('should render users list after loading', async () => {
    // Mock fetch
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve([
            {
              id: 1,
              name: 'John Doe',
              email: 'john@example.com',
              username: 'johndoe',
            },
          ]),
      })
    ) as jest.Mock;

    render(<HomeScreen />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeTruthy();
      expect(screen.getByText('john@example.com')).toBeTruthy();
      expect(screen.getByText('@johndoe')).toBeTruthy();
    });
  });

  it('should show error state on API failure', async () => {
    global.fetch = jest.fn(() =>
      Promise.reject(new Error('API Error'))
    ) as jest.Mock;

    render(<HomeScreen />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText('Error loading users')).toBeTruthy();
    });
  });
});
