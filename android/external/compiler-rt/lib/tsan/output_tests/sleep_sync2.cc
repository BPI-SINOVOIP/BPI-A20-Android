#include <pthread.h>
#include <unistd.h>

int X = 0;

void *Thread(void *p) {
  X = 42;
  return 0;
}

int main() {
  pthread_t t;
  usleep(100*1000);
  pthread_create(&t, 0, Thread, 0);
  X = 43;
  pthread_join(t, 0);
  return 0;
}

// CHECK: WARNING: ThreadSanitizer: data race
// CHECK-NOT: As if synchronized via sleep
