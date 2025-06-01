import { useState, useEffect, useCallback, useRef } from 'react';

interface UseDebounceInputOptions {
  initialValue?: string;
  delay?: number;
}

// Custom hook for debounced text input handling to reduce render frequency
export function useDebounceInput({ initialValue = '', delay = 300 }: UseDebounceInputOptions = {}) {
  // Immediate state for UI responsiveness
  const [inputValue, setInputValue] = useState(initialValue);
  // Debounced state for expensive operations
  const [debouncedValue, setDebouncedValue] = useState(initialValue);
  const timerRef = useRef<number | null>(null);

  // Clean up timer on unmount
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, []);

  // Update debounced value after delay
  useEffect(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    timerRef.current = window.setTimeout(() => {
      setDebouncedValue(inputValue);
    }, delay);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [inputValue, delay]);

  // Memoized onChange handler to prevent re-renders
  const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setInputValue(e.target.value);
  }, []);

  // Reset function
  const reset = useCallback((value: string = '') => {
    setInputValue(value);
    setDebouncedValue(value);
  }, []);

  return {
    value: inputValue,
    debouncedValue,
    handleChange,
    setValue: setInputValue,
    reset
  };
}
