import { apiClient } from '@/utils/api';

describe('apiClient', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should make a successful API call', async () => {
    const mockData = { id: 1, name: 'Test' };
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockData),
      })
    ) as jest.Mock;

    const result = await apiClient('/test');
    expect(result).toEqual(mockData);
    expect(global.fetch).toHaveBeenCalledWith(
      'https://jsonplaceholder.typicode.com/test',
      expect.objectContaining({
        headers: {
          'Content-Type': 'application/json',
        },
      })
    );
  });

  it('should throw error on failed API call', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: false,
        status: 404,
        statusText: 'Not Found',
      })
    ) as jest.Mock;

    await expect(apiClient('/test')).rejects.toThrow('API Error: 404 Not Found');
  });

  it('should handle POST requests', async () => {
    const mockData = { id: 1, name: 'Created' };
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockData),
      })
    ) as jest.Mock;

    const result = await apiClient('/test', {
      method: 'POST',
      body: JSON.stringify({ name: 'Test' }),
    });

    expect(result).toEqual(mockData);
    expect(global.fetch).toHaveBeenCalledWith(
      'https://jsonplaceholder.typicode.com/test',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ name: 'Test' }),
      })
    );
  });
});
